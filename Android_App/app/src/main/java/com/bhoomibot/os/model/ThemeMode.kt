package com.bhoomibot.os.model

// App theme preference. The industrial UI ships dark by default; a light option is offered in Settings.
enum class ThemeMode {
    DARK,
    LIGHT
}

// Persisted string keys for ThemeMode.
fun ThemeMode.toKey(): String = name
fun String?.toThemeMode(): ThemeMode = if (this == ThemeMode.LIGHT.name) ThemeMode.LIGHT else ThemeMode.DARK
