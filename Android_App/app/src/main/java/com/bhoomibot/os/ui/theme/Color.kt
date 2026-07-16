package com.bhoomibot.os.ui.theme

import androidx.compose.ui.graphics.Color

// ===== App-wide color palette (all custom colors used across every screen) =====

// Darkest base color: the app background behind everything (near-black blue).
val HmiBackground = Color(0xFF091218)

// Slightly lighter surface used for cards and panels that sit on top of the background.
val HmiSurface = Color(0xFF13232D)

// Even lighter surface used for raised/secondary cards (e.g. status cards, toggles).
val HmiSurfaceVariant = Color(0xFF1A303C)

// Default text color on dark surfaces (almost white) for high readability in sunlight.
val HmiText = Color(0xFFF1F5F4)

// Secondary/dimmed text color used for labels, hints, and inactive values.
val MutedText = Color(0xFF9FB0B8)

// Primary brand / "all good" color: green used for ON states, active controls, and positive status.
val SignalGreen = Color(0xFF47D16C)

// Secondary accent color (blue) used for the "Manual" mode badge and highlights.
val SteelBlue = Color(0xFF63B3ED)

// Danger color: red used for Emergency Stop, errors, and OFF/disconnected status.
val SafetyRed = Color(0xFFE8504F)

// Warning color: amber used for cautionary states (reserved for future use).
val WarningAmber = Color(0xFFFFB547)
