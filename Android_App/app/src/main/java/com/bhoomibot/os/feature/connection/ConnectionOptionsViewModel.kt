package com.bhoomibot.os.feature.connection

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bhoomibot.os.connection.model.ConnectionConfig
import com.bhoomibot.os.connection.model.VideoQuality
import com.bhoomibot.os.connection.transport.local.LocalHubManager
import com.bhoomibot.os.data.DevicePreferences
import com.bhoomibot.os.data.LiveLinkPreferences
import com.bhoomibot.os.data.LiveLinkPreferencesStore
import com.bhoomibot.os.model.DeviceRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Holds the editable live-link config.
 */
class ConnectionOptionsViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ConnectionOptionsUiState())
    val uiState: StateFlow<ConnectionOptionsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val prefs = LiveLinkPreferencesStore.preferences(getApplication()).first()
            val role = DevicePreferences.role(application).first() ?: DeviceRole.OPERATOR
            
            // Start with current local hub state to avoid race condition
            _uiState.value = ConnectionOptionsUiState(
                role = role,
                serverUrl = prefs.serverUrl,
                robotId = prefs.robotId,
                sessionCode = prefs.sessionCode,
                autoReconnect = prefs.autoReconnect,
                networkMode = runCatching { PhoneNetworkMode.valueOf(prefs.networkMode) }
                    .getOrDefault(PhoneNetworkMode.INTERNET),
                videoFps = prefs.videoFps,
                videoQuality = runCatching { VideoQuality.valueOf(prefs.videoQuality) }
                    .getOrDefault(VideoQuality.MEDIUM),
                recentServerUrls = prefs.recentServerUrls,
                localHubActive = LocalHubManager.isServerRunning.value,
                localIpAddress = LocalHubManager.localIpAddress.value
            )
        }

        // Collect Local Hub state
        viewModelScope.launch {
            LocalHubManager.isServerRunning.collectLatest { running ->
                _uiState.value = _uiState.value.copy(localHubActive = running)
            }
        }
        viewModelScope.launch {
            LocalHubManager.localIpAddress.collectLatest { ip ->
                _uiState.value = _uiState.value.copy(localIpAddress = ip)
            }
        }
    }

    fun setServerUrl(v: String) { _uiState.value = _uiState.value.copy(serverUrl = v.trim()) }

    /** Pick a previously used server URL (from the history chips). */
    fun selectRecentServerUrl(url: String) { setServerUrl(url) }
    fun setRobotId(v: String) { _uiState.value = _uiState.value.copy(robotId = v.trim()) }
    fun setSessionCode(v: String) { _uiState.value = _uiState.value.copy(sessionCode = v.trim()) }
    fun setAutoReconnect(v: Boolean) { _uiState.value = _uiState.value.copy(autoReconnect = v) }
    fun setNetworkMode(v: PhoneNetworkMode) { _uiState.value = _uiState.value.copy(networkMode = v, saved = false) }
    fun setVideoFps(v: Int) { _uiState.value = _uiState.value.copy(videoFps = v.coerceIn(1, 30)) }
    fun setVideoQuality(q: VideoQuality) { _uiState.value = _uiState.value.copy(videoQuality = q) }

    /** Generates a compact string for QR pairing. */
    fun getPairingData(): String {
        val s = _uiState.value
        val ip = LocalHubManager.localIpAddress.value
        return "BHOOMI_V1|$ip|${s.robotId}|${s.sessionCode}"
    }

    /** Decodes a QR code and fills the fields. */
    fun processScannedData(data: String) {
        if (!data.startsWith("BHOOMI_V1|")) return
        val parts = data.split("|")
        if (parts.size < 4) return
        
        val ip = parts[1]
        val robotId = parts[2]
        val session = parts[3]
        
        _uiState.update { it.copy(
            serverUrl = "ws://$ip:8080",
            robotId = robotId,
            sessionCode = session
        ) }
    }

    /** Toggles the local hub server on the Robot Phone */
    fun toggleLocalHub() {
        val currentlyActive = _uiState.value.localHubActive
        android.util.Log.d("LocalHub", "Toggling Hub. Current state: $currentlyActive")
        if (currentlyActive) {
            LocalHubManager.stopServer()
            _uiState.value = _uiState.value.copy(localHubActive = false)
        } else {
            android.util.Log.d("LocalHub", "Starting server...")
            LocalHubManager.startServer(getApplication())
            // Suggest the local URL immediately
            setServerUrl("ws://localhost:8080")
            _uiState.value = _uiState.value.copy(localHubActive = true)
        }
    }

    /** Persists the current values; the screen then navigates to the live screen. */
    fun save(onSaved: () -> Unit = {}) {
        viewModelScope.launch {
            val s = _uiState.value
            // Persist the normalized (wss://) URL so the live screens read a usable
            // address even if the user typed https://.
            LiveLinkPreferencesStore.save(
                getApplication(),
                LiveLinkPreferences(
                    serverUrl = s.normalizedServerUrl,
                    robotId = s.robotId,
                    sessionCode = s.sessionCode,
                    autoReconnect = s.autoReconnect,
                    networkMode = s.networkMode.name,
                    videoFps = s.videoFps,
                    videoQuality = s.videoQuality.name
                )
            )
            // Remember this URL for quick re-selection next time (most-recent-first).
            LiveLinkPreferencesStore.addRecentServerUrl(getApplication(), s.normalizedServerUrl)
            val recents = (listOf(s.normalizedServerUrl) +
                s.recentServerUrls.filter { it != s.normalizedServerUrl }).take(5)
            _uiState.value = s.copy(saved = true, recentServerUrls = recents)
            android.util.Log.d(
                "BhoomiBotRelay",
                "[GUI] save() persisted serverUrl='${s.normalizedServerUrl}' robotId='${s.robotId}' " +
                    "session='${s.sessionCode}' role=${s.role}; invoking onStart()"
            )
            onSaved()
        }
    }

    /** Builds the [ConnectionConfig] the live screens connect with (role comes from onboarding). */
    fun toConfig(): ConnectionConfig {
        val s = _uiState.value
        return ConnectionConfig(
            serverUrl = s.normalizedServerUrl,
            robotId = s.robotId,
            sessionCode = s.sessionCode,
            role = s.role,
            autoReconnect = s.autoReconnect,
            videoFps = s.videoFps,
            videoQuality = s.videoQuality
        )
    }
}
