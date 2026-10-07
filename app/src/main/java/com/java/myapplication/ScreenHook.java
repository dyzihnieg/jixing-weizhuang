package com.java.myapplication;

import android.content.SharedPreferences;
import android.app.Application;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Display;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import io.github.libxposed.api.XposedModule;
import io.github.libxposed.api.XposedInterface;

public final class ScreenHook extends XposedModule {
    private SharedPreferences prefs;
    private boolean initialized;
    private int installedHooks;
    private boolean logged;
    private boolean configurationLogged;
    private boolean characteristicsLogged;
    private String targetPackage;
    private Field[] baselineMetricFields;
    private boolean baselineFieldsResolved;

    @Override public void onPackageReady(PackageReadyParam param) {
        String name = param.getPackageName();
        if (initialized || name.equals("android") || name.equals("com.android.systemui")
                || name.equals("com.java.myapplication")) return;
        try {
            prefs = getRemotePreferences("screen_mask");
        } catch (Throwable t) {
            android.util.Log.e("DeviceMask", "Remote preferences unavailable; hooks not installed", t);
            return;
        }
        initialized = true;
        targetPackage = name;
        android.util.Log.i("DeviceMask", "Screen config package=" + name + " enabled=" + enabled()
                + " width=" + prefs.getInt("width", 0) + " height=" + prefs.getInt("height", 0)
                + " dpi=" + prefs.getInt("density", 0));
        if (prefs.getBoolean("soc_enabled", false) && Build.VERSION.SDK_INT >= 31) {
            try {
                for (String field : new String[]{"SOC_MODEL", "SOC_MANUFACTURER"}) {
                    java.lang.reflect.Field f = Build.class.getDeclaredField(field);
                    f.setAccessible(true);
                    f.set(null, prefs.getString(field.equals("SOC_MODEL") ? "soc_model" : "soc_vendor", ""));
                }
            } catch (Throwable t) { android.util.Log.e("DeviceMask", "SoC fields", t); }
        }

        install(Resources.class, "getDisplayMetrics", chain -> {
            Object result = chain.proceed();
            if (enabled() && result instanceof DisplayMetrics) {
                DisplayMetrics copy = new DisplayMetrics();
                copy.setTo((DisplayMetrics) result);
                rewrite(copy);
                return copy;
            }
            return result;
        });
        install(Resources.class, "getConfiguration", chain -> {
            Object result = chain.proceed();
            if (!enabled() || !(result instanceof Configuration)) return result;
            Configuration copy = new Configuration((Configuration) result);
            rewrite(copy);
            logConfiguration("query", (Configuration) result, copy);
            hit();
            return copy;
        });
        if (name.equals("com.tencent.mm")) installWeChatSignals();
        for (String method : new String[]{"getMetrics", "getRealMetrics"}) {
            install(Display.class, method, chain -> {
                Object result = chain.proceed();
                if (enabled() && chain.getArgs().get(0) instanceof DisplayMetrics)
                    rewrite((DisplayMetrics) chain.getArgs().get(0));
                return result;
            }, DisplayMetrics.class);
        }
        for (String method : new String[]{"getSize", "getRealSize"}) {
            install(Display.class, method, chain -> {
                Object result = chain.proceed();
                if (enabled() && chain.getArgs().get(0) instanceof Point) {
                    Point p = (Point) chain.getArgs().get(0);
                    int[] size = dimensions(p.x > p.y);
                    p.set(size[0], size[1]);
                    hit();
                }
                return result;
            }, Point.class);
        }
        install(Display.class, "getRectSize", chain -> {
            Object result = chain.proceed();
            if (enabled() && chain.getArgs().get(0) instanceof Rect) {
                Rect r = (Rect) chain.getArgs().get(0);
                int[] size = dimensions(r.width() > r.height());
                r.set(0, 0, size[0], size[1]);
                hit();
            }
            return result;
        }, Rect.class);
        for (String method : new String[]{"getWidth", "getHeight"}) {
            install(Display.class, method, chain -> {
                Object result = chain.proceed();
                if (!enabled()) return result;
                int rotation = ((Display) chain.getThisObject()).getRotation();
                hit();
                return dimensions(rotation == 1 || rotation == 3)[method.equals("getWidth") ? 0 : 1];
            });
        }
        for (String method : new String[]{"getPhysicalWidth", "getPhysicalHeight"}) {
            install(Display.Mode.class, method, chain -> {
                Object result = chain.proceed();
                if (!enabled()) return result;
                hit();
                return prefs.getInt(method.equals("getPhysicalWidth") ? "width" : "height", (Integer) result);
            });
        }
        if (Build.VERSION.SDK_INT >= 30) {
            try {
                Class<?> type = Class.forName("android.view.WindowMetrics");
                install(type, "getBounds", chain -> {
                    Object result = chain.proceed();
                    if (!enabled() || !(result instanceof Rect)) return result;
                    Rect r = (Rect) result;
                    int[] size = dimensions(r.width() > r.height());
                    hit();
                    return new Rect(r.left, r.top, r.left + size[0], r.top + size[1]);
                });
                if (Build.VERSION.SDK_INT >= 34) install(type, "getDensity", chain -> {
                    Object result = chain.proceed();
                    return enabled() ? prefs.getInt("density", 420) / 160f : result;
                });
            } catch (Throwable t) { android.util.Log.e("DeviceMask", "WindowMetrics", t); }
        }
        android.util.Log.i("DeviceMask", "API102 loaded " + name + "; screen=" + enabled()
                + "; installedHooks=" + installedHooks);
        if (installedHooks == 0)
            android.util.Log.e("DeviceMask", "No screen hooks installed for " + name);
    }

    private void install(Class<?> type, String name, XposedInterface.Hooker callback, Class<?>... args) {
        try {
            Method method = type.getDeclaredMethod(name, args);
            install(method, callback);
        } catch (Throwable t) {
            android.util.Log.e("DeviceMask", "Hook " + type.getName() + "." + name, t);
        }
    }

    private void install(Method method, XposedInterface.Hooker callback) {
        hook(method).intercept(callback);
        installedHooks++;
    }

    private void installWeChatSignals() {
        // Returning a Configuration copy does not update AssetManager's resource
        // qualifiers. Align WeChat's resources before attachBaseContext/onCreate.
        install(Application.class, "attach", chain -> {
            if (enabled() && chain.getArg(0) instanceof Context) {
                try {
                    Resources resources = ((Context) chain.getArg(0)).getResources();
                    Configuration config = new Configuration(resources.getConfiguration());
                    DisplayMetrics metrics = new DisplayMetrics();
                    metrics.setTo(resources.getDisplayMetrics());
                    rewrite(config);
                    rewrite(metrics);
                    alignResourceMetricBaseline(metrics);
                    resources.updateConfiguration(config, metrics);
                    android.util.Log.i("DeviceMask", "WeChat resource qualifiers aligned before attach");
                } catch (Throwable t) {
                    android.util.Log.e("DeviceMask", "WeChat initial resource alignment", t);
                }
            }
            return chain.proceed();
        }, Context.class);
        try {
            Class<?> implementation = Class.forName("android.content.res.ResourcesImpl");
            for (Method method : implementation.getDeclaredMethods()) {
                if (!method.getName().equals("updateConfiguration")) continue;
                Class<?>[] types = method.getParameterTypes();
                if (types.length == 0 || types[0] != Configuration.class) continue;
                install(method, chain -> {
                    if (!enabled()) return chain.proceed();
                    Object[] args = chain.getArgs().toArray();
                    for (int i = 0; i < args.length; i++) {
                        if (args[i] instanceof Configuration) {
                            Configuration original = (Configuration) args[i];
                            Configuration copy = new Configuration(original);
                            rewrite(copy);
                            logConfiguration("resource-update", original, copy);
                            args[i] = copy;
                        } else if (args[i] instanceof DisplayMetrics) {
                            DisplayMetrics copy = new DisplayMetrics();
                            copy.setTo((DisplayMetrics) args[i]);
                            rewrite(copy);
                            alignResourceMetricBaseline(copy);
                            args[i] = copy;
                        }
                    }
                    return chain.proceed(args);
                });
            }
        } catch (Throwable t) {
            android.util.Log.e("DeviceMask", "WeChat resource update hook", t);
        }
        try {
            Class<?> properties = Class.forName("android.os.SystemProperties");
            XposedInterface.Hooker callback = chain -> {
                Object result = chain.proceed();
                if (!enabled() || !"ro.build.characteristics".equals(chain.getArg(0))
                        || !ScreenClassification.isTablet(prefs.getInt("width", 0),
                        prefs.getInt("height", 0), prefs.getInt("density", 0))) return result;
                String value = ScreenClassification.tabletCharacteristics((String) result);
                if (!characteristicsLogged) {
                    characteristicsLogged = true;
                    android.util.Log.i("DeviceMask", "WeChat characteristics " + result + " -> " + value);
                }
                return value;
            };
            install(properties, "get", callback, String.class);
            install(properties, "get", callback, String.class, String.class);
        } catch (Throwable t) {
            android.util.Log.e("DeviceMask", "WeChat characteristics hook", t);
        }
    }

    private synchronized void alignResourceMetricBaseline(DisplayMetrics metrics) {
        // CompatibilityInfo resets the public metrics from these baseline fields
        // during a ResourcesImpl update. Keep that baseline in the same preset.
        if (!baselineFieldsResolved) {
            baselineFieldsResolved = true;
            String[] names = {"noncompatWidthPixels", "noncompatHeightPixels", "noncompatDensityDpi",
                    "noncompatDensity", "noncompatScaledDensity", "noncompatXdpi", "noncompatYdpi"};
            baselineMetricFields = new Field[names.length];
            for (int i = 0; i < names.length; i++) {
                try {
                    Field field = DisplayMetrics.class.getDeclaredField(names[i]);
                    field.setAccessible(true);
                    baselineMetricFields[i] = field;
                } catch (Throwable t) {
                    android.util.Log.e("DeviceMask", "Resource metric baseline " + names[i], t);
                }
            }
        }
        try {
            for (int i = 0; i < baselineMetricFields.length; i++) {
                Field field = baselineMetricFields[i];
                if (field == null) continue;
                switch (i) {
                    case 0: field.setInt(metrics, metrics.widthPixels); break;
                    case 1: field.setInt(metrics, metrics.heightPixels); break;
                    case 2: field.setInt(metrics, metrics.densityDpi); break;
                    case 3: field.setFloat(metrics, metrics.density); break;
                    case 4: field.setFloat(metrics, metrics.scaledDensity); break;
                    case 5: field.setFloat(metrics, metrics.xdpi); break;
                    case 6: field.setFloat(metrics, metrics.ydpi); break;
                }
            }
        } catch (IllegalAccessException t) {
            android.util.Log.e("DeviceMask", "Resource metric baseline update", t);
        }
    }

    private void logConfiguration(String source, Configuration original, Configuration rewritten) {
        if (configurationLogged) return;
        configurationLogged = true;
        android.util.Log.i("DeviceMask", "Screen class package=" + targetPackage + " source=" + source
                + " profile=" + prefs.getString("profile", "")
                + " dp=" + rewritten.screenWidthDp + "x" + rewritten.screenHeightDp
                + " sw=" + rewritten.smallestScreenWidthDp
                + " layout=0x" + Integer.toHexString(original.screenLayout)
                + "->0x" + Integer.toHexString(rewritten.screenLayout));
    }

    private boolean enabled() {
        return prefs != null && prefs.getBoolean("enabled", false)
                && prefs.getInt("width", 0) >= 320 && prefs.getInt("width", 0) <= 7680
                && prefs.getInt("height", 0) >= 320 && prefs.getInt("height", 0) <= 7680
                && prefs.getInt("density", 0) >= 120 && prefs.getInt("density", 0) <= 960;
    }

    private int[] dimensions(boolean landscape) {
        int w = prefs.getInt("width", 1080), h = prefs.getInt("height", 2400);
        return landscape ? new int[]{h, w} : new int[]{w, h};
    }

    private void rewrite(DisplayMetrics m) {
        int[] size = dimensions(m.widthPixels > m.heightPixels);
        float fontScale = m.density > 0 ? m.scaledDensity / m.density : 1f;
        int dpi = prefs.getInt("density", 420);
        m.widthPixels = size[0];
        m.heightPixels = size[1];
        m.densityDpi = dpi;
        m.density = dpi / 160f;
        m.scaledDensity = m.density * fontScale;
        m.xdpi = dpi;
        m.ydpi = dpi;
        hit();
    }

    private void rewrite(Configuration c) {
        int[] size = dimensions(c.orientation == Configuration.ORIENTATION_LANDSCAPE);
        int dpi = prefs.getInt("density", 420);
        c.densityDpi = dpi;
        c.screenWidthDp = Math.round(size[0] * 160f / dpi);
        c.screenHeightDp = Math.round(size[1] * 160f / dpi);
        c.smallestScreenWidthDp = Math.min(c.screenWidthDp, c.screenHeightDp);
        c.screenLayout = ScreenClassification.screenLayout(c.screenLayout, c.screenWidthDp, c.screenHeightDp);
    }

    private void hit() {
        if (!logged) {
            logged = true;
            android.util.Log.i("DeviceMask", "API102 screen query hit");
        }
    }
}
