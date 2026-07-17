/**
 * App-level configuration store (device role, theme, language).
 *
 * One of THREE separate Jetpack DataStore instances in this app (the others hold VCU connection
 * prefs and relay live-link prefs). DataStore is used instead of SharedPreferences because it is
 * coroutine-first and lifecycle-aware. No network is involved. `role` returns null until the user
 * picks one on Onboarding.
 */
package com.bhoomibot.os.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bhoomibot.os.model.DeviceRole
import com.bhoomibot.os.model.ThemeMode
import com.bhoomibot.os.model.toDeviceRole
import com.bhoomibot.os.model.toKey
import com.bhoomibot.os.model.toThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Single local store for app-level configuration (device role, theme, language).
// DataStore is used (not SharedPreferences) because it is coroutine-first, lifecycle-aware,
// and easy to extend later with logs/settings. No network is involved.
private val Context.dataStore by preferencesDataStore(name = "bhoomibot_device_prefs")

object DevicePreferences {
    private val ROLE_KEY = stringPreferencesKey("device_role")        // null => not chosen yet (onboarding)
    private val THEME_KEY = stringPreferencesKey("theme_mode")        // "DARK" | "LIGHT"
    private val LANGUAGE_KEY = stringPreferencesKey("language_code")   // "en" (infra ready for more)

    // Emits the selected role, or null when the user hasn't chosen one yet.
    fun role(context: Context): Flow<DeviceRole?> =
        context.dataStore.data.map { it[ROLE_KEY]?.toDeviceRole() }

    // Emits the theme mode (defaults to DARK when unset).
    fun themeMode(context: Context): Flow<ThemeMode> =
        context.dataStore.data.map { it[THEME_KEY].toThemeMode() }

    // Emits the language code (defaults to "en").
    fun languageCode(context: Context): Flow<String> =
        context.dataStore.data.map { it[LANGUAGE_KEY] ?: "en" }

    suspend fun setRole(context: Context, role: DeviceRole) {
        context.dataStore.edit { it[ROLE_KEY] = role.toKey() }
    }

    suspend fun clearRole(context: Context) {
        context.dataStore.edit { it.remove(ROLE_KEY) }
    }

    suspend fun setThemeMode(context: Context, mode: ThemeMode) {
        context.dataStore.edit { it[THEME_KEY] = mode.toKey() }
    }

    suspend fun setLanguageCode(context: Context, code: String) {
        context.dataStore.edit { it[LANGUAGE_KEY] = code }
    }
}
