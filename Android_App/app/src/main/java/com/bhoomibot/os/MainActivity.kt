package com.bhoomibot.os

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import com.bhoomibot.os.service.BhoomiBotService
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

    // Apply the persisted language BEFORE resources are loaded.
    // NOTE: runBlocking is used because attachBaseContext is synchronous.
    // This is safe because language preference is a small local DataStore value.
    override fun attachBaseContext(newBase: Context) {
        val code = try {
            runBlocking { DevicePreferences.languageCode(newBase).first() }
        } catch (e: Exception) {
            "en"
        }
        val locale = Locale(code)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Start the safety background monitor
        com.bhoomibot.os.feature.autonomous.AutonomyManager.getSafetyMonitor(application)

        // AI-Fix: Request Bluetooth Permissions for Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val permissions = arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            val missing = permissions.filter { 
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED 
            }
            if (missing.isNotEmpty()) {
                registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {}.launch(missing.toTypedArray())
            }
        }

        // Android 13+ Notification Permission request
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                registerForActivityResult(ActivityResultContracts.RequestPermission()) {}.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

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
