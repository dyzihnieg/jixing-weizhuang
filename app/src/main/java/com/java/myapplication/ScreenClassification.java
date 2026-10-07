package com.java.myapplication;

import java.util.LinkedHashSet;
import java.util.Set;

/** Screen-class signals derived from the same dimensions used by the display hooks. */
final class ScreenClassification {
    private ScreenClassification() {}

    static int screenLayout(int original, int widthDp, int heightDp) {
        if (widthDp <= 0 || heightDp <= 0) return original;
        int shortDp = Math.min(widthDp, heightDp), longDp = Math.max(widthDp, heightDp);
        int size, aspect;
        boolean compatible;
        // AOSP Configuration.reduceScreenLayout rules, recomputed from a reset
        // classification so a copied phone layout can increase to tablet size.
        if (longDp < 470) {
            size = 1; // SCREENLAYOUT_SIZE_SMALL
            aspect = 0x10; // SCREENLAYOUT_LONG_NO
            compatible = false;
        } else {
            size = longDp >= 960 && shortDp >= 720 ? 4
                    : longDp >= 640 && shortDp >= 480 ? 3 : 2;
            aspect = longDp * 3L / 5 >= shortDp - 1 ? 0x20 : 0x10;
            compatible = shortDp > 321 || longDp > 570;
        }
        return (original & ~(0x0f | 0x30 | 0x10000000))
                | size | aspect | (compatible ? 0x10000000 : 0);
    }

    static boolean isTablet(int widthPx, int heightPx, int densityDpi) {
        return densityDpi > 0 && Math.min(widthPx, heightPx) * 160L >= 600L * densityDpi;
    }

    static String tabletCharacteristics(String original) {
        Set<String> tokens = new LinkedHashSet<>();
        tokens.add("tablet");
        if (original != null) {
            for (String token : original.split(",")) {
                String value = token.trim();
                if (!value.isEmpty() && !value.equals("phone") && !value.equals("default")) tokens.add(value);
            }
        }
        return String.join(",", tokens);
    }
}
