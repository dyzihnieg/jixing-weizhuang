package com.java.myapplication.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

private data class AccentPalette(
    val lightPrimary: Color,
    val lightContainer: Color,
    val lightOnContainer: Color,
    val darkPrimary: Color,
    val darkOnPrimary: Color,
    val darkContainer: Color,
    val darkOnContainer: Color,
)

private val TealPalette = AccentPalette(
    lightPrimary = Color(0xFF006B5E),
    lightContainer = Color(0xFF9FF3E3),
    lightOnContainer = Color(0xFF00201A),
    darkPrimary = Color(0xFF7AD7C7),
    darkOnPrimary = Color(0xFF00382F),
    darkContainer = Color(0xFF005046),
    darkOnContainer = Color(0xFF9FF3E3),
)

private val BluePalette = AccentPalette(
    lightPrimary = Color(0xFF3267A8),
    lightContainer = Color(0xFFD3E4FF),
    lightOnContainer = Color(0xFF001C38),
    darkPrimary = Color(0xFF9DCAFF),
    darkOnPrimary = Color(0xFF003259),
    darkContainer = Color(0xFF164F85),
    darkOnContainer = Color(0xFFD3E4FF),
)

private val RosePalette = AccentPalette(
    lightPrimary = Color(0xFFAA3C5C),
    lightContainer = Color(0xFFFFD9E3),
    lightOnContainer = Color(0xFF3E0017),
    darkPrimary = Color(0xFFFFB0C6),
    darkOnPrimary = Color(0xFF650A2E),
    darkContainer = Color(0xFF882344),
    darkOnContainer = Color(0xFFFFD9E3),
)

private val AmberPalette = AccentPalette(
    lightPrimary = Color(0xFF795900),
    lightContainer = Color(0xFFFFE09B),
    lightOnContainer = Color(0xFF261900),
    darkPrimary = Color(0xFFF0C75E),
    darkOnPrimary = Color(0xFF402D00),
    darkContainer = Color(0xFF5C4300),
    darkOnContainer = Color(0xFFFFE09B),
)

private val RedPalette = AccentPalette(
    lightPrimary = Color(0xFFAD3435),
    lightContainer = Color(0xFFFFDAD6),
    lightOnContainer = Color(0xFF410002),
    darkPrimary = Color(0xFFFFB3B0),
    darkOnPrimary = Color(0xFF680F15),
    darkContainer = Color(0xFF8B2529),
    darkOnContainer = Color(0xFFFFDAD6),
)

private val BlackPalette = AccentPalette(
    lightPrimary = Color(0xFF24262B),
    lightContainer = Color(0xFFE2E4EA),
    lightOnContainer = Color(0xFF191B20),
    darkPrimary = Color(0xFFD6D9E2),
    darkOnPrimary = Color(0xFF2E3036),
    darkContainer = Color(0xFF44474F),
    darkOnContainer = Color(0xFFE3E5EC),
)

private val PurplePalette = AccentPalette(
    lightPrimary = Color(0xFF6750A4),
    lightContainer = Color(0xFFE9DDFF),
    lightOnContainer = Color(0xFF22005D),
    darkPrimary = Color(0xFFCFBCFF),
    darkOnPrimary = Color(0xFF381E72),
    darkContainer = Color(0xFF4F378B),
    darkOnContainer = Color(0xFFEADDFF),
)

fun accentPreviewColor(accent: AccentColor): Color = when (accent) {
    AccentColor.TEAL -> Color(0xFF16A98E)
    AccentColor.BLUE -> Color(0xFF4A89FF)
    AccentColor.ROSE -> Color(0xFFEF82B2)
    AccentColor.AMBER -> Color(0xFFF2C84B)
    AccentColor.RED -> Color(0xFFE85656)
    AccentColor.BLACK -> Color(0xFF292C33)
    AccentColor.PURPLE -> Color(0xFF9A7CEA)
}

private val LightColors = lightColorScheme(
    secondary = Color(0xFF47637A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCE7FF),
    onSecondaryContainer = Color(0xFF001E30),
    tertiary = Color(0xFF805A30),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDDBB),
    onTertiaryContainer = Color(0xFF2C1700),
    background = Color(0xFFF6F8F8),
    onBackground = Color(0xFF191D1E),
    surface = Color(0xFFF6F8F8),
    onSurface = Color(0xFF191D1E),
    surfaceVariant = Color(0xFFE2E7E7),
    onSurfaceVariant = Color(0xFF424849),
    inverseSurface = Color(0xFF2D3233),
    inverseOnSurface = Color(0xFFEDF1F1),
    outline = Color(0xFF737A7A),
    outlineVariant = Color(0xFFC2C9C9),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    scrim = Color.Black,
    surfaceBright = Color(0xFFF6F8F8),
    surfaceDim = Color(0xFFD7DCDD),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF0F4F4),
    surfaceContainer = Color(0xFFEAEEEE),
    surfaceContainerHigh = Color(0xFFE4E9E9),
    surfaceContainerHighest = Color(0xFFDEE3E3),
    secondaryFixed = Color(0xFFCCE7FF),
    secondaryFixedDim = Color(0xFFAECBE5),
    onSecondaryFixed = Color(0xFF001E30),
    onSecondaryFixedVariant = Color(0xFF2E485E),
    tertiaryFixed = Color(0xFFFFDDBB),
    tertiaryFixedDim = Color(0xFFF2BF8A),
    onTertiaryFixed = Color(0xFF2C1700),
    onTertiaryFixedVariant = Color(0xFF64411B),
)

private val DarkColors = darkColorScheme(
    secondary = Color(0xFFAECBE5),
    onSecondary = Color(0xFF173247),
    secondaryContainer = Color(0xFF2E485E),
    onSecondaryContainer = Color(0xFFCCE7FF),
    tertiary = Color(0xFFF2BF8A),
    onTertiary = Color(0xFF482909),
    tertiaryContainer = Color(0xFF64411B),
    onTertiaryContainer = Color(0xFFFFDDBB),
    background = Color(0xFF101415),
    onBackground = Color(0xFFDEE3E3),
    surface = Color(0xFF101415),
    onSurface = Color(0xFFDEE3E3),
    surfaceVariant = Color(0xFF424849),
    onSurfaceVariant = Color(0xFFC2C9C9),
    inverseSurface = Color(0xFFDEE3E3),
    inverseOnSurface = Color(0xFF2D3233),
    outline = Color(0xFF8C9393),
    outlineVariant = Color(0xFF424849),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    scrim = Color.Black,
    surfaceBright = Color(0xFF363A3B),
    surfaceDim = Color(0xFF101415),
    surfaceContainerLowest = Color(0xFF0B0F10),
    surfaceContainerLow = Color(0xFF191D1E),
    surfaceContainer = Color(0xFF1D2122),
    surfaceContainerHigh = Color(0xFF272B2C),
    surfaceContainerHighest = Color(0xFF323637),
    secondaryFixed = Color(0xFFCCE7FF),
    secondaryFixedDim = Color(0xFFAECBE5),
    onSecondaryFixed = Color(0xFF001E30),
    onSecondaryFixedVariant = Color(0xFF2E485E),
    tertiaryFixed = Color(0xFFFFDDBB),
    tertiaryFixedDim = Color(0xFFF2BF8A),
    onTertiaryFixed = Color(0xFF2C1700),
    onTertiaryFixedVariant = Color(0xFF64411B),
)

private fun accentPalette(accent: AccentColor): AccentPalette = when (accent) {
    AccentColor.TEAL -> TealPalette
    AccentColor.BLUE -> BluePalette
    AccentColor.ROSE -> RosePalette
    AccentColor.AMBER -> AmberPalette
    AccentColor.RED -> RedPalette
    AccentColor.BLACK -> BlackPalette
    AccentColor.PURPLE -> PurplePalette
}

internal fun appearanceColorScheme(settings: AppearanceSettings, dark: Boolean): ColorScheme {
    val palette = accentPalette(settings.accent)
    val supportingPalette = accentPalette(
        if (settings.colorStyle == ColorStyle.SPLIT) settings.secondaryAccent else settings.accent,
    )
    val colors = if (dark) DarkColors else LightColors
    val primary = if (dark) palette.darkPrimary else palette.lightPrimary
    val supporting = if (dark) supportingPalette.darkPrimary else supportingPalette.lightPrimary
    val onSupporting = if (dark) supportingPalette.darkOnPrimary else Color.White
    val supportingContainer = if (dark) supportingPalette.darkContainer else supportingPalette.lightContainer
    val onSupportingContainer = if (dark) supportingPalette.darkOnContainer else supportingPalette.lightOnContainer
    val surfaceAccent = if (primary == supporting) primary else lerp(primary, supporting, 0.5f)
    fun tintedSurface(base: Color): Color = lerp(base, surfaceAccent, if (dark) 0.025f else 0.03f)

    return colors.copy(
        primary = primary,
        onPrimary = if (dark) palette.darkOnPrimary else Color.White,
        primaryContainer = if (dark) palette.darkContainer else palette.lightContainer,
        onPrimaryContainer = if (dark) palette.darkOnContainer else palette.lightOnContainer,
        inversePrimary = if (dark) palette.lightPrimary else palette.darkPrimary,
        surfaceTint = primary,
        primaryFixed = palette.lightContainer,
        primaryFixedDim = palette.darkPrimary,
        onPrimaryFixed = palette.lightOnContainer,
        onPrimaryFixedVariant = palette.darkContainer,
        secondary = supporting,
        onSecondary = onSupporting,
        secondaryContainer = supportingContainer,
        onSecondaryContainer = onSupportingContainer,
        tertiary = supporting,
        onTertiary = onSupporting,
        tertiaryContainer = supportingContainer,
        onTertiaryContainer = onSupportingContainer,
        secondaryFixed = supportingPalette.lightContainer,
        secondaryFixedDim = supportingPalette.darkPrimary,
        onSecondaryFixed = supportingPalette.lightOnContainer,
        onSecondaryFixedVariant = supportingPalette.darkContainer,
        tertiaryFixed = supportingPalette.lightContainer,
        tertiaryFixedDim = supportingPalette.darkPrimary,
        onTertiaryFixed = supportingPalette.lightOnContainer,
        onTertiaryFixedVariant = supportingPalette.darkContainer,
        background = tintedSurface(colors.background),
        surface = tintedSurface(colors.surface),
        surfaceVariant = tintedSurface(colors.surfaceVariant),
        surfaceBright = tintedSurface(colors.surfaceBright),
        surfaceDim = tintedSurface(colors.surfaceDim),
        surfaceContainerLowest = tintedSurface(colors.surfaceContainerLowest),
        surfaceContainerLow = tintedSurface(colors.surfaceContainerLow),
        surfaceContainer = tintedSurface(colors.surfaceContainer),
        surfaceContainerHigh = tintedSurface(colors.surfaceContainerHigh),
        surfaceContainerHighest = tintedSurface(colors.surfaceContainerHighest),
    )
}
