package com.bhoomibot.os

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.bhoomibot.os.data.DevicePreferences
import com.bhoomibot.os.model.ThemeMode
import com.bhoomibot.os.navigation.AppNavigation
import com.bhoomibot.os.ui.theme.BhoomiBotTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.Locale

// The single Android Activity that hosts the entire app (single-activity + Compose Navigation).
// It is responsible for: applying the stored language (via context wrap), applying the stored
// theme, and hosting the navigation graph that routes to Onboarding / Operator / Robot homes.
class MainActivity : ComponentActivity() {

    // Drives the active theme; updated from DevicePreferences once the flow emits.
    private var darkTheme by mutableStateOf(true)

    // Apply the persisted language BEFORE resources are loaded, so stringResource() resolves
    // in the correct locale. DataStore is read synchronously here because attachBaseContext
    // cannot suspend.
    override fun attachBaseContext(newBase: Context) {
        val code = runBlocking { DevicePreferences.languageCode(newBase).first() }
        val locale = Locale(code)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Observe theme preference and re-compose the theme when it changes.
        lifecycleScope.launch {
            DevicePreferences.themeMode(this@MainActivity).collect { mode ->
                darkTheme = (mode == ThemeMode.DARK)
            }
        }

        // Build the Compose UI: apply the (dark/light) "BhoomiBot" theme, then start navigation.
        setContent { BhoomiBotTheme(darkTheme = darkTheme) { AppNavigation() } }
    }

    // Used by Settings ("Restart Application" / language change). Recreate re-runs
    // attachBaseContext so both theme and language are re-applied cleanly.
    fun restartApplication() = recreate()
}
