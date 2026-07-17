package com.bhoomibot.os.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val IndustrialDarkScheme = darkColorScheme(
    primary = SignalGreen,
    onPrimary = Color(0xFF062112),
    secondary = SteelBlue,
    background = HmiBackground,
    surface = HmiSurface,
    surfaceVariant = HmiSurfaceVariant,
    onBackground = HmiText,
    onSurface = HmiText,
    error = SafetyRed
)

/** Application theme intentionally remains dark for high-contrast field operation. */
@Composable
fun BhoomiBotTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = IndustrialDarkScheme, typography = Typography, content = content)
}
