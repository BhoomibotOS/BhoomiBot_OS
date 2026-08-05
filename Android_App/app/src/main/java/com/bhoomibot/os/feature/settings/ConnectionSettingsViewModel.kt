package com.bhoomibot.os.feature.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bhoomibot.os.data.ConnectionPreferencesStore
import com.bhoomibot.os.vcu.ConnectionManager
import com.bhoomibot.os.vcu.ConnectionPreferences
import com.bhoomibot.os.vcu.ConnectionType
import com.bhoomibot.os.repository.provideRobotRepository
import com.bhoomibot.os.service.BhoomiBotService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Holds the editable [ConnectionPreferences] for the VCU connection.
 */
class ConnectionSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = provideRobotRepository(application)

    private val _prefs = MutableStateFlow(ConnectionPreferences())
    val prefs: StateFlow<ConnectionPreferences> = _prefs.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

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
        viewModelScope.launch {
            repository.isConnected.collectLatest { connected ->
                _isConnected.value = connected
            }
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

    // Triggers real-time CONNECT or DISCONNECT
    fun toggleConnection() {
        viewModelScope.launch {
            if (_isConnected.value) {
                repository.disconnect()
                BhoomiBotService.stop(getApplication())
            } else {
                // Save current edits first
                ConnectionPreferencesStore.save(getApplication(), _prefs.value)
                BhoomiBotService.start(getApplication())
                repository.sendDriveCommand(com.bhoomibot.os.model.DriveCommand.STOP)
            }
        }
    }
}
