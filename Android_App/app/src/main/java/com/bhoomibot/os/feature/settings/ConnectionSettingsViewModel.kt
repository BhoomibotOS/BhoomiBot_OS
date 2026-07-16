package com.bhoomibot.os.feature.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bhoomibot.os.data.ConnectionPreferencesStore
import com.bhoomibot.os.vcu.ConnectionManager
import com.bhoomibot.os.vcu.ConnectionPreferences
import com.bhoomibot.os.vcu.ConnectionType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Holds the editable [ConnectionPreferences] for the VCU connection (BT MAC, WiFi host/port,
 * mode, timeouts). Values live in memory while the screen is open and are persisted to DataStore
 * only when [save] is called, so a stray edit is never written until the operator confirms.
 */
class ConnectionSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val _prefs = MutableStateFlow(ConnectionPreferences())
    val prefs: StateFlow<ConnectionPreferences> = _prefs.asStateFlow()

    // Result text of the last "Test connection" attempt; null until one is run.
    private val _testStatus = MutableStateFlow<String?>(null)
    val testStatus: StateFlow<String?> = _testStatus.asStateFlow()

    // True while a connection test is in flight (disables the test button).
    private val _isTesting = MutableStateFlow(false)
    val isTesting: StateFlow<Boolean> = _isTesting.asStateFlow()

    // Load any previously saved preferences when the ViewModel is first created.
    init {
        viewModelScope.launch {
            _prefs.value = ConnectionPreferencesStore.preferences(application.applicationContext).first()
        }
    }

    fun setConnectionType(type: ConnectionType) { _prefs.value = _prefs.value.copy(connectionType = type) }
    fun setBluetoothMac(address: String) { _prefs.value = _prefs.value.copy(bluetoothMacAddress = address.trim()) }
    fun setWifiHost(host: String) { _prefs.value = _prefs.value.copy(wifiHost = host.trim()) }
    fun setWifiPort(port: Int) { _prefs.value = _prefs.value.copy(wifiPort = port.coerceIn(1, 65535)) }
    fun setAutoReconnect(enabled: Boolean) { _prefs.value = _prefs.value.copy(isAutoReconnect = enabled) }
    fun setConnectionTimeout(ms: Int) { _prefs.value = _prefs.value.copy(connectionTimeoutMs = ms.coerceAtLeast(500)) }

    // Persist the current values to DataStore and stamp lastConnectedAt as the save time.
    fun save(onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            ConnectionPreferencesStore.save(
                getApplication(),
                _prefs.value.copy(lastConnectedAt = System.currentTimeMillis())
            )
            onSaved()
        }
    }

    // Attempts a real connection using the current (unsaved) preferences: opens the socket and
    // sends a probe command, then reports success/failure with the underlying error message.
    fun testConnection() {
        viewModelScope.launch {
            _isTesting.value = true
            _testStatus.value = null
            val result = runCatching {
                val mgr = ConnectionManager(getApplication(), _prefs.value)
                try {
                    mgr.connect()
                    mgr.send("T") // probe: forces the socket open and verifies the link
                } finally {
                    mgr.disconnect()
                }
            }
            _testStatus.value = if (result.isSuccess) {
                "Connected successfully"
            } else {
                "Failed: ${result.exceptionOrNull()?.message ?: "unknown error"}"
            }
            _isTesting.value = false
        }
    }
}
