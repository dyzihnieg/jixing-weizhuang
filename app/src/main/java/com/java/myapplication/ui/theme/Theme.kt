package com.java.myapplication.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

val LocalAppearanceSettings = staticCompositionLocalOf { AppearanceSettings() }

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(8.dp),
)

@Composable
fun MyApplicationTheme(
    settings: AppearanceSettings = AppearanceSettings(),
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalAppearanceSettings provides settings) {
        MaterialTheme(
            colorScheme = appearanceColorScheme(
                settings = settings,
                dark = settings.themeMode.isDark(isSystemInDarkTheme()),
            ),
            typography = Typography,
            shapes = AppShapes,
            content = content,
        )
    }
}
