package com.java.myapplication.ui.theme

import android.content.Context
import android.content.SharedPreferences

class AppearanceStore internal constructor(private val preferences: SharedPreferences) {
    constructor(context: Context) : this(
        context.applicationContext.getSharedPreferences("appearance", Context.MODE_PRIVATE),
    )

    fun read(): AppearanceSettings = AppearanceSettings(
        themeMode = ThemeMode.fromStored(preferences.getString("theme_mode", null)),
        accent = AccentColor.fromStored(preferences.getString("accent", null)),
        colorStyle = ColorStyle.fromStored(preferences.getString("color_style", null)),
        secondaryAccent = AccentColor.fromStored(
            preferences.getString("secondary_accent", null),
            fallback = AccentColor.BLUE,
        ),
        animationSpeed = AnimationSpeed.fromStored(preferences.getString("animation_speed", null)),
    )

    fun save(settings: AppearanceSettings) {
        preferences.edit()
            .putString("theme_mode", settings.themeMode.storageKey)
            .putString("accent", settings.accent.storageKey)
            .putString("color_style", settings.colorStyle.storageKey)
            .putString("secondary_accent", settings.secondaryAccent.storageKey)
            .putString("animation_speed", settings.animationSpeed.storageKey)
            .apply()
    }
}
