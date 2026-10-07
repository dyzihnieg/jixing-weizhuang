package com.java.myapplication.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppearanceColorSchemeTest {
    @Test
    fun redBlueSplitUsesRedPrimaryAndBlueSupportingRoles() {
        listOf(false, true).forEach { dark ->
            val red = appearanceColorScheme(AppearanceSettings(accent = AccentColor.RED), dark)
            val blue = appearanceColorScheme(AppearanceSettings(accent = AccentColor.BLUE), dark)
            val split = appearanceColorScheme(
                AppearanceSettings(
                    accent = AccentColor.RED,
                    colorStyle = ColorStyle.SPLIT,
                    secondaryAccent = AccentColor.BLUE,
                ),
                dark,
            )

            assertEquals(red.primary, split.primary)
            assertEquals(red.primaryContainer, split.primaryContainer)
            assertEquals(blue.primary, split.secondary)
            assertEquals(blue.primaryContainer, split.secondaryContainer)
            assertEquals(blue.primary, split.tertiary)
            assertEquals(blue.primaryContainer, split.tertiaryContainer)
            assertNotEquals(split.primary, split.secondary)
        }
    }

    @Test
    fun solidModeIgnoresTheSavedSecondaryColor() {
        listOf(false, true).forEach { dark ->
            val first = appearanceColorScheme(
                AppearanceSettings(accent = AccentColor.RED, secondaryAccent = AccentColor.BLUE),
                dark,
            )
            val second = appearanceColorScheme(
                AppearanceSettings(accent = AccentColor.RED, secondaryAccent = AccentColor.BLACK),
                dark,
            )

            assertEquals(first.primary, second.primary)
            assertEquals(first.secondary, second.secondary)
            assertEquals(first.tertiary, second.tertiary)
            assertEquals(first.surface, second.surface)
        }
    }

    @Test
    fun matchingSplitColorsProduceTheSamePaletteAsSolid() {
        listOf(false, true).forEach { dark ->
            AccentColor.entries.forEach { accent ->
                val solid = appearanceColorScheme(AppearanceSettings(accent = accent), dark)
                val split = appearanceColorScheme(
                    AppearanceSettings(accent = accent, colorStyle = ColorStyle.SPLIT, secondaryAccent = accent),
                    dark,
                )

                assertEquals(solid.primary, split.primary)
                assertEquals(solid.secondary, split.secondary)
                assertEquals(solid.surface, split.surface)
            }
        }
    }

    @Test
    fun blackUsesCharcoalInLightModeAndVisibleSilverInDarkMode() {
        val settings = AppearanceSettings(accent = AccentColor.BLACK)
        val light = appearanceColorScheme(settings, dark = false)
        val dark = appearanceColorScheme(settings, dark = true)

        assertTrue(light.primary.luminance() < 0.05f)
        assertTrue(dark.primary.luminance() > 0.60f)
        assertTrue(contrastRatio(light.primary, light.surface) >= 3f)
        assertTrue(contrastRatio(dark.primary, dark.surface) >= 3f)
    }

    @Test
    fun everyPaletteKeepsReadableTextAcrossBothThemeModesAndSplitCombinations() {
        listOf(false, true).forEach { dark ->
            AccentColor.entries.forEach { primary ->
                AccentColor.entries.forEach { secondary ->
                    ColorStyle.entries.forEach { style ->
                        val colors = appearanceColorScheme(
                            AppearanceSettings(accent = primary, colorStyle = style, secondaryAccent = secondary),
                            dark,
                        )
                        val foregroundsAndBackgrounds = mapOf(
                            "primary" to (colors.onPrimary to colors.primary),
                            "primaryContainer" to (colors.onPrimaryContainer to colors.primaryContainer),
                            "secondary" to (colors.onSecondary to colors.secondary),
                            "secondaryContainer" to (colors.onSecondaryContainer to colors.secondaryContainer),
                            "tertiary" to (colors.onTertiary to colors.tertiary),
                            "tertiaryContainer" to (colors.onTertiaryContainer to colors.tertiaryContainer),
                            "surface" to (colors.onSurface to colors.surface),
                            "surfaceVariant" to (colors.onSurfaceVariant to colors.surfaceVariant),
                            "surfaceContainerHighest" to (colors.onSurface to colors.surfaceContainerHighest),
                            "background" to (colors.onBackground to colors.background),
                        )

                        foregroundsAndBackgrounds.forEach { (role, pair) ->
                            val contrast = contrastRatio(pair.first, pair.second)
                            assertTrue(
                                "$primary/$secondary $style dark=$dark $role contrast=$contrast",
                                contrast >= 4.5f,
                            )
                        }
                    }
                }
            }
        }
    }

    private fun contrastRatio(first: Color, second: Color): Float {
        val firstLuminance = first.luminance()
        val secondLuminance = second.luminance()
        return (maxOf(firstLuminance, secondLuminance) + 0.05f) /
            (minOf(firstLuminance, secondLuminance) + 0.05f)
    }
}
