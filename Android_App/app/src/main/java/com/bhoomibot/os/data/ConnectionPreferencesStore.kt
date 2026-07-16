package com.bhoomibot.os.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.bhoomibot.os.vcu.ConnectionPreferenceKeys
import com.bhoomibot.os.vcu.ConnectionPreferences
import com.bhoomibot.os.vcu.ConnectionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Persisted connection settings (BT MAC, WiFi host/port, connection mode, ...).
// Kept in its own store (separate from DevicePreferences) so connection config can evolve
// independently of app-level config. Read via [preferences]; written via [save].
private val Context.connectionDataStore by preferencesDataStore(name = "bhoomibot_connection_prefs")

object ConnectionPreferencesStore {
    // Emits the current connection preferences, falling back to sensible defaults when a key
    // is missing or holds an unparseable value (e.g. a renamed enum entry).
    fun preferences(context: Context): Flow<ConnectionPreferences> =
        context.connectionDataStore.data.map { p ->
            ConnectionPreferences(
                connectionType = runCatching {
                    ConnectionType.valueOf(p[ConnectionPreferenceKeys.CONNECTION_TYPE] ?: ConnectionType.BLUETOOTH.name)
                }.getOrDefault(ConnectionType.BLUETOOTH),
                bluetoothMacAddress = p[ConnectionPreferenceKeys.BLUETOOTH_MAC_ADDRESS] ?: "",
                wifiHost = p[ConnectionPreferenceKeys.WIFI_HOST] ?: "",
                wifiPort = p[ConnectionPreferenceKeys.WIFI_PORT] ?: 8888,
                isAutoReconnect = p[ConnectionPreferenceKeys.AUTO_RECONNECT] ?: true,
                lastConnectedAt = p[ConnectionPreferenceKeys.LAST_CONNECTED_AT] ?: 0L,
                connectionTimeoutMs = p[ConnectionPreferenceKeys.CONNECTION_TIMEOUT_MS] ?: 5000
            )
        }

    // Persists the full preferences object back to DataStore.
    suspend fun save(context: Context, prefs: ConnectionPreferences) {
        context.connectionDataStore.edit { p ->
            p[ConnectionPreferenceKeys.CONNECTION_TYPE] = prefs.connectionType.name
            p[ConnectionPreferenceKeys.BLUETOOTH_MAC_ADDRESS] = prefs.bluetoothMacAddress
            p[ConnectionPreferenceKeys.WIFI_HOST] = prefs.wifiHost
            p[ConnectionPreferenceKeys.WIFI_PORT] = prefs.wifiPort
            p[ConnectionPreferenceKeys.AUTO_RECONNECT] = prefs.isAutoReconnect
            p[ConnectionPreferenceKeys.LAST_CONNECTED_AT] = prefs.lastConnectedAt
            p[ConnectionPreferenceKeys.CONNECTION_TIMEOUT_MS] = prefs.connectionTimeoutMs
        }
    }
}
