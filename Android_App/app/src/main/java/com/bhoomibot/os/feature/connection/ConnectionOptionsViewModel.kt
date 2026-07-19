package com.bhoomibot.os.feature.connection

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bhoomibot.os.connection.model.ConnectionConfig
import com.bhoomibot.os.connection.model.VideoQuality
import com.bhoomibot.os.data.DevicePreferences
import com.bhoomibot.os.data.LiveLinkPreferences
import com.bhoomibot.os.data.LiveLinkPreferencesStore
import com.bhoomibot.os.model.DeviceRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Holds the editable live-link config (server URL, Robot ID, session code,
 * video quality, auto-reconnect). Loads the saved values on start and persists
 * them via [LiveLinkPreferencesStore] when [save] is called — mirroring the
 * existing [com.bhoomibot.os.feature.settings.ConnectionSettingsViewModel].
 */
class ConnectionOptionsViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(ConnectionOptionsUiState())
    val uiState: StateFlow<ConnectionOptionsUiState> = _uiState.asStateFlow()

    // Load the saved live-link preferences (and the onboarding role) once, when created.
    // Enums are stored as their String name(); runCatching { valueOf(...) }.getOrDefault(...)
    // falls back safely if a stored value is missing or from an older/renamed enum.
    init {
        viewModelScope.launch {
            val prefs = LiveLinkPreferencesStore.preferences(getApplication()).first()
            val role = DevicePreferences.role(application).first() ?: DeviceRole.OPERATOR
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
                    .getOrDefault(VideoQuality.MEDIUM)
            )
        }
    }

    fun setServerUrl(v: String) { _uiState.value = _uiState.value.copy(serverUrl = v.trim()) }
    fun setRobotId(v: String) { _uiState.value = _uiState.value.copy(robotId = v.trim()) }
    fun setSessionCode(v: String) { _uiState.value = _uiState.value.copy(sessionCode = v.trim()) }
    fun setAutoReconnect(v: Boolean) { _uiState.value = _uiState.value.copy(autoReconnect = v) }
    fun setNetworkMode(v: PhoneNetworkMode) { _uiState.value = _uiState.value.copy(networkMode = v, saved = false) }
    fun setVideoFps(v: Int) { _uiState.value = _uiState.value.copy(videoFps = v.coerceIn(1, 30)) }
    fun setVideoQuality(q: VideoQuality) { _uiState.value = _uiState.value.copy(videoQuality = q) }

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
            _uiState.value = s.copy(saved = true)
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
