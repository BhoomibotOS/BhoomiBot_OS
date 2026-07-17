package com.bhoomibot.os.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Type.kt — the app-wide Material 3 typography, passed into MaterialTheme by Theme.kt.
 *
 * Only bodyLarge is customized; all other text styles (titles, labels, headlines, etc.)
 * fall back to the Material 3 defaults. The commented-out block below shows the standard
 * scaffolding for overriding additional styles if that's ever needed.
 */

// Set of Material typography styles to start with.
// Only the base body text is customized here; every other text style uses Material 3 defaults.
// This keeps all screens consistent in font size/weight across the app.
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
    /* Other default text styles to override
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
    */
)