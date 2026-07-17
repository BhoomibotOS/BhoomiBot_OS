package com.bhoomibot.os.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Theme.kt — defines the app's Material 3 theme.
 *
 * It builds two color schemes (dark + light) from the custom palette in Color.kt and
 * exposes the [BhoomiBotTheme] composable that every screen wraps its content in.
 * The active scheme is chosen by the darkTheme flag, which flows from DevicePreferences
 * through MainActivity down into this composable.
 */

// The dark color scheme used by the whole app (high contrast for outdoor/sunlight use).
// Each line maps a Material 3 role to one of our custom colors from Color.kt:
//   primary        -> SignalGreen : main brand color (buttons, active switches, progress)
//   onPrimary      -> dark green  : text/icon color drawn ON TOP of green (e.g. on green buttons)
//   secondary      -> SteelBlue   : secondary accent (used for the "Manual" mode label)
//   background     -> HmiBackground : app background behind everything
//   surface        -> HmiSurface  : cards/panels sitting on the background
//   surfaceVariant -> HmiSurfaceVariant : raised/secondary cards
//   onBackground   -> HmiText     : normal text color on dark areas
//   onSurface      -> HmiText     : normal text color on cards
//   error          -> SafetyRed   : error / danger color (Emergency Stop, disconnect)
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

// Light scheme: same brand colors tuned for bright surfaces (offered in Settings).
private val IndustrialLightScheme = lightColorScheme(
    primary = Color(0xFF1F8A3B),
    onPrimary = Color.White,
    secondary = SteelBlue,
    background = Color(0xFFF4F7F6),
    surface = Color.White,
    surfaceVariant = Color(0xFFE2EAE8),
    onBackground = Color(0xFF0E1A1F),
    onSurface = Color(0xFF0E1A1F),
    error = SafetyRed
)

/** Application theme. Dark by default for field operation; a light option is selectable in Settings.
 *  Wrap every screen's content in BhoomiBotTheme { ... } to apply the color scheme + typography.
 *  @param darkTheme when true uses the industrial dark scheme, otherwise the light scheme. */
@Composable
fun BhoomiBotTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    val colorScheme = if (darkTheme) IndustrialDarkScheme else IndustrialLightScheme
    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
