package com.java.myapplication.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private fun appTextStyle(
    fontSize: Int,
    lineHeight: Int,
    weight: FontWeight = FontWeight.Normal,
) = TextStyle(
    fontFamily = FontFamily.Default,
    fontWeight = weight,
    fontSize = fontSize.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = 0.sp,
)

val Typography = Typography(
    displayLarge = appTextStyle(48, 56),
    displayMedium = appTextStyle(40, 48),
    displaySmall = appTextStyle(34, 42),
    headlineLarge = appTextStyle(30, 38, FontWeight.Medium),
    headlineMedium = appTextStyle(26, 34, FontWeight.Medium),
    headlineSmall = appTextStyle(22, 30, FontWeight.Medium),
    titleLarge = appTextStyle(20, 28, FontWeight.Medium),
    titleMedium = appTextStyle(16, 24, FontWeight.Medium),
    titleSmall = appTextStyle(14, 20, FontWeight.Medium),
    bodyLarge = appTextStyle(16, 24),
    bodyMedium = appTextStyle(14, 22),
    bodySmall = appTextStyle(12, 18),
    labelLarge = appTextStyle(14, 20, FontWeight.Medium),
    labelMedium = appTextStyle(12, 18, FontWeight.Medium),
    labelSmall = appTextStyle(11, 16, FontWeight.Medium),
)
