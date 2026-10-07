package com.java.myapplication.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppearancePreferencesTest {
    @Test
    fun themeModeFollowsSystemOrUsesExplicitSelection() {
        assertFalse(ThemeMode.SYSTEM.isDark(systemDark = false))
        assertTrue(ThemeMode.SYSTEM.isDark(systemDark = true))
        assertFalse(ThemeMode.LIGHT.isDark(systemDark = false))
        assertFalse(ThemeMode.LIGHT.isDark(systemDark = true))
        assertTrue(ThemeMode.DARK.isDark(systemDark = false))
        assertTrue(ThemeMode.DARK.isDark(systemDark = true))
    }

    @Test
    fun storedThemeModesUseStableKeys() {
        val storedModes = mapOf(
            "system" to ThemeMode.SYSTEM,
            "light" to ThemeMode.LIGHT,
            "dark" to ThemeMode.DARK,
        )

        storedModes.forEach { (key, mode) ->
            assertEquals(key, mode.storageKey)
            assertEquals(mode, ThemeMode.fromStored(key))
        }
    }

    @Test
    fun missingOrUnknownThemeModeFollowsSystem() {
        listOf(null, "", "future_mode", "DARK").forEach { stored ->
            assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStored(stored))
        }
    }

    @Test
    fun storedAccentColorsUseStableKeys() {
        val storedColors = mapOf(
            "teal" to AccentColor.TEAL,
            "blue" to AccentColor.BLUE,
            "rose" to AccentColor.ROSE,
            "amber" to AccentColor.AMBER,
            "red" to AccentColor.RED,
            "black" to AccentColor.BLACK,
            "purple" to AccentColor.PURPLE,
        )

        storedColors.forEach { (key, accent) ->
            assertEquals(key, accent.storageKey)
            assertEquals(accent, AccentColor.fromStored(key))
        }
    }

    @Test
    fun missingOrUnknownAccentColorUsesTeal() {
        listOf(null, "", "future_accent", "BLUE").forEach { stored ->
            assertEquals(AccentColor.TEAL, AccentColor.fromStored(stored))
        }
    }

    @Test
    fun storedRedAccentRemainsRedWhenRestored() {
        assertEquals("red", AccentColor.fromStored("red").storageKey)
    }

    @Test
    fun storedBlackAccentRemainsBlackWhenRestored() {
        assertEquals("black", AccentColor.fromStored("black").storageKey)
    }

    @Test
    fun storedPurpleAccentRemainsPurpleWhenRestored() {
        assertEquals("purple", AccentColor.fromStored("purple").storageKey)
    }

    @Test
    fun freshAppearanceFollowsSystemWithTealAccent() {
        val settings = AppearanceSettings()

        assertEquals(ThemeMode.SYSTEM, settings.themeMode)
        assertEquals(AccentColor.TEAL, settings.accent)
        assertEquals(ColorStyle.SOLID, settings.colorStyle)
        assertEquals(AccentColor.BLUE, settings.secondaryAccent)
        assertEquals(AnimationSpeed.STANDARD, settings.animationSpeed)
    }

    @Test
    fun storedColorStylesUseStableKeys() {
        assertEquals(ColorStyle.SOLID, ColorStyle.fromStored("solid"))
        assertEquals(ColorStyle.SPLIT, ColorStyle.fromStored("split"))
        assertEquals("solid", ColorStyle.SOLID.storageKey)
        assertEquals("split", ColorStyle.SPLIT.storageKey)
    }

    @Test
    fun missingOrUnknownColorStyleUsesSolid() {
        listOf(null, "", "future_style", "SPLIT").forEach { stored ->
            assertEquals(ColorStyle.SOLID, ColorStyle.fromStored(stored))
        }
    }

    @Test
    fun storedAnimationSpeedRestoresItsConfiguredDuration() {
        val storedSpeeds = mapOf(
            "slow" to (AnimationSpeed.SLOW to 520),
            "standard" to (AnimationSpeed.STANDARD to 320),
            "fast" to (AnimationSpeed.FAST to 180),
        )

        storedSpeeds.forEach { (key, expected) ->
            val restored = AnimationSpeed.fromStored(key)

            assertEquals(expected.first, restored)
            assertEquals(key, restored.storageKey)
            assertEquals(expected.second, restored.durationMillis)
        }
    }

    @Test
    fun missingOrUnknownAnimationSpeedUsesStandard() {
        listOf(null, "", "future_speed", "FAST", " fast ").forEach { stored ->
            assertEquals(AnimationSpeed.STANDARD, AnimationSpeed.fromStored(stored))
        }
    }
}
