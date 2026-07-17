/**
 * App theme preference (DARK or LIGHT).
 *
 * The industrial UI ships dark by default for high contrast in sunlight; a light option is offered
 * in Settings. Persisted as a stable string via `toKey()`/`toThemeMode()`.
 */
package com.bhoomibot.os.model

// App theme preference. The industrial UI ships dark by default; a light option is offered in Settings.
enum class ThemeMode {
    DARK,
    LIGHT
}

// Persisted string keys for ThemeMode.
fun ThemeMode.toKey(): String = name
fun String?.toThemeMode(): ThemeMode = if (this == ThemeMode.LIGHT.name) ThemeMode.LIGHT else ThemeMode.DARK
