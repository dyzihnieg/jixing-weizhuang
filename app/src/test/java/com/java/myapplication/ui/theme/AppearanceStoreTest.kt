package com.java.myapplication.ui.theme

import android.content.SharedPreferences
import java.lang.reflect.Proxy
import org.junit.Assert.assertEquals
import org.junit.Test

class AppearanceStoreTest {
    @Test
    fun defaultAnimationSpeedIsWrittenWhenLegacySettingsAreSaved() {
        val preferences = inMemoryPreferences(mapOf("accent" to "rose"))
        val store = AppearanceStore(preferences)

        store.save(store.read())

        assertEquals("standard", preferences.getString("animation_speed", null))
    }

    @Test
    fun legacyPreferencesKeepModeAndAccentWithSolidColorDefaults() {
        val preferences = inMemoryPreferences(
            mapOf("theme_mode" to "dark", "accent" to "rose"),
        )

        assertEquals(
            AppearanceSettings(
                themeMode = ThemeMode.DARK,
                accent = AccentColor.ROSE,
                colorStyle = ColorStyle.SOLID,
                secondaryAccent = AccentColor.BLUE,
                animationSpeed = AnimationSpeed.STANDARD,
            ),
            AppearanceStore(preferences).read(),
        )
    }

    @Test
    fun saveUsesStableKeysAndCanBeReadImmediatelyByANewStore() {
        val preferences = inMemoryPreferences()
        val settings = AppearanceSettings(
            themeMode = ThemeMode.LIGHT,
            accent = AccentColor.RED,
            colorStyle = ColorStyle.SPLIT,
            secondaryAccent = AccentColor.BLACK,
        )

        AppearanceStore(preferences).save(settings)

        assertEquals("light", preferences.getString("theme_mode", null))
        assertEquals("red", preferences.getString("accent", null))
        assertEquals("split", preferences.getString("color_style", null))
        assertEquals("black", preferences.getString("secondary_accent", null))
        assertEquals(settings, AppearanceStore(preferences).read())
    }

    @Test
    fun switchingToSolidPreservesTheChosenSecondaryAccent() {
        val preferences = inMemoryPreferences()
        val store = AppearanceStore(preferences)
        val split = AppearanceSettings(
            accent = AccentColor.RED,
            colorStyle = ColorStyle.SPLIT,
            secondaryAccent = AccentColor.PURPLE,
        )
        store.save(split)

        store.save(split.copy(colorStyle = ColorStyle.SOLID))

        assertEquals(ColorStyle.SOLID, AppearanceStore(preferences).read().colorStyle)
        assertEquals(AccentColor.PURPLE, AppearanceStore(preferences).read().secondaryAccent)
    }

    @Test
    fun missingOrUnknownSecondaryAccentUsesBlueWithoutChangingPrimary() {
        listOf(null, "", "future_accent", "BLUE").forEach { stored ->
            val values = mutableMapOf("accent" to "red", "color_style" to "split")
            if (stored != null) values["secondary_accent"] = stored

            val settings = AppearanceStore(inMemoryPreferences(values)).read()

            assertEquals(AccentColor.RED, settings.accent)
            assertEquals(AccentColor.BLUE, settings.secondaryAccent)
            assertEquals(ColorStyle.SPLIT, settings.colorStyle)
        }
    }

    @Test
    fun unknownStoredValuesFallBackToFreshAppearance() {
        val preferences = inMemoryPreferences(
            mapOf(
                "theme_mode" to "future_mode",
                "accent" to "future_accent",
                "color_style" to "future_style",
                "secondary_accent" to "future_accent",
                "animation_speed" to "future_speed",
            ),
        )

        assertEquals(AppearanceSettings(), AppearanceStore(preferences).read())
    }

    @Test
    fun eachAnimationSpeedUsesItsStableKeyAndRestoresInANewStore() {
        val storedSpeeds = mapOf(
            AnimationSpeed.SLOW to "slow",
            AnimationSpeed.STANDARD to "standard",
            AnimationSpeed.FAST to "fast",
        )

        storedSpeeds.forEach { (speed, key) ->
            val preferences = inMemoryPreferences()
            val settings = AppearanceSettings(animationSpeed = speed)

            AppearanceStore(preferences).save(settings)

            assertEquals(key, preferences.getString("animation_speed", null))
            assertEquals(settings, AppearanceStore(preferences).read())
        }
    }

    @Test
    fun legacyOrInvalidSpeedUsesStandardWithoutResettingColors() {
        listOf(null, "", "future_speed", "FAST", " fast ").forEach { stored ->
            val values = mutableMapOf(
                "theme_mode" to "dark",
                "accent" to "red",
                "color_style" to "split",
                "secondary_accent" to "blue",
            )
            if (stored != null) values["animation_speed"] = stored

            assertEquals(
                AppearanceSettings(
                    themeMode = ThemeMode.DARK,
                    accent = AccentColor.RED,
                    colorStyle = ColorStyle.SPLIT,
                    secondaryAccent = AccentColor.BLUE,
                    animationSpeed = AnimationSpeed.STANDARD,
                ),
                AppearanceStore(inMemoryPreferences(values)).read(),
            )
        }
    }

    @Test
    fun changingColorsPreservesTheSavedAnimationSpeed() {
        listOf(AnimationSpeed.SLOW, AnimationSpeed.FAST).forEach { speed ->
            val preferences = inMemoryPreferences()
            val store = AppearanceStore(preferences)
            store.save(AppearanceSettings(animationSpeed = speed))

            store.save(
                store.read().copy(
                    themeMode = ThemeMode.DARK,
                    accent = AccentColor.RED,
                    colorStyle = ColorStyle.SPLIT,
                    secondaryAccent = AccentColor.PURPLE,
                ),
            )

            assertEquals(
                AppearanceSettings(
                    themeMode = ThemeMode.DARK,
                    accent = AccentColor.RED,
                    colorStyle = ColorStyle.SPLIT,
                    secondaryAccent = AccentColor.PURPLE,
                    animationSpeed = speed,
                ),
                AppearanceStore(preferences).read(),
            )
        }
    }

    @Test
    fun changingAnimationSpeedPreservesAllSavedColorSettings() {
        val preferences = inMemoryPreferences()
        val store = AppearanceStore(preferences)
        store.save(
            AppearanceSettings(
                themeMode = ThemeMode.DARK,
                accent = AccentColor.RED,
                colorStyle = ColorStyle.SPLIT,
                secondaryAccent = AccentColor.PURPLE,
            ),
        )

        listOf(AnimationSpeed.SLOW, AnimationSpeed.FAST, AnimationSpeed.STANDARD).forEach { speed ->
            store.save(store.read().copy(animationSpeed = speed))

            assertEquals(
                AppearanceSettings(
                    themeMode = ThemeMode.DARK,
                    accent = AccentColor.RED,
                    colorStyle = ColorStyle.SPLIT,
                    secondaryAccent = AccentColor.PURPLE,
                    animationSpeed = speed,
                ),
                AppearanceStore(preferences).read(),
            )
        }
    }

    // Android owns disk IO; this stand-in retains the preference boundary's immediate
    // in-memory writes so these JVM tests exercise the store's real key mapping.
    private fun inMemoryPreferences(initial: Map<String, String> = emptyMap()): SharedPreferences {
        val values = initial.toMutableMap()
        return Proxy.newProxyInstance(
            SharedPreferences::class.java.classLoader,
            arrayOf(SharedPreferences::class.java),
        ) { _, method, arguments ->
            when (method.name) {
                "getString" -> values[arguments!![0] as String] ?: arguments[1]
                "edit" -> {
                    val pending = mutableMapOf<String, String?>()
                    lateinit var editor: SharedPreferences.Editor
                    editor = Proxy.newProxyInstance(
                        SharedPreferences.Editor::class.java.classLoader,
                        arrayOf(SharedPreferences.Editor::class.java),
                    ) { _, editorMethod, editorArguments ->
                        when (editorMethod.name) {
                            "putString" -> {
                                pending[editorArguments!![0] as String] = editorArguments[1] as String?
                                editor
                            }
                            "apply", "commit" -> {
                                pending.forEach { (key, value) ->
                                    if (value == null) values.remove(key) else values[key] = value
                                }
                                if (editorMethod.name == "commit") true else null
                            }
                            else -> error("Unexpected editor operation: ${editorMethod.name}")
                        }
                    } as SharedPreferences.Editor
                    editor
                }
                else -> error("Unexpected preference operation: ${method.name}")
            }
        } as SharedPreferences
    }
}
