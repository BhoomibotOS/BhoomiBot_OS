/**
 * Connection preferences for the LOCAL VCU / ESP32 link.
 *
 * Plain data class ([ConnectionPreferences]) plus the DataStore [ConnectionPreferenceKeys]. Holds
 * the chosen [ConnectionType], Bluetooth MAC, Wi-Fi host/port, auto-reconnect flag, last-connected
 * timestamp, and connect timeout. Persisted by `data/ConnectionPreferencesStore.kt`.
 */
package com.bhoomibot.os.vcu

import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey

/**
 * Data class to hold all connection preferences for ESP32/VCU
 * Stored in DataStore for persistence across app restarts
 */
data class ConnectionPreferences(
    val connectionType: ConnectionType = ConnectionType.BLUETOOTH,
    val bluetoothMacAddress: String = "",
    val wifiHost: String = "",
    val wifiPort: Int = 8888,
    val isAutoReconnect: Boolean = true,
    val lastConnectedAt: Long = 0L,
    val connectionTimeoutMs: Int = 5000
)

/**
 * DataStore keys for connection preferences
 */
object ConnectionPreferenceKeys {
    val CONNECTION_TYPE = stringPreferencesKey("connection_type")
    val BLUETOOTH_MAC_ADDRESS = stringPreferencesKey("bluetooth_mac_address")
    val WIFI_HOST = stringPreferencesKey("wifi_host")
    val WIFI_PORT = intPreferencesKey("wifi_port")
    val AUTO_RECONNECT = booleanPreferencesKey("auto_reconnect")
    val LAST_CONNECTED_AT = longPreferencesKey("last_connected_at")
    val CONNECTION_TIMEOUT_MS = intPreferencesKey("connection_timeout_ms")
}