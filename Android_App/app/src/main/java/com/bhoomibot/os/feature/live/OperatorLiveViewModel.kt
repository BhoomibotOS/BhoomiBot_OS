// ===========================================================================
// OperatorLiveViewModel.kt
// ---------------------------------------------------------------------------
// Backs OperatorLiveScreen. Owns the operator's live-link session and the
// screen's state. It:
//   1. loads this phone's live-link settings, connects as the OPERATOR role,
//   2. collects the repository's flows (connection/peer/telemetry/frames) into
//      one immutable OperatorLiveUiState exposed via StateFlow,
//   3. decodes inbound jpeg frames into a Compose ImageBitmap, and
//   4. sends drive / e-stop / PTO / lights commands back to the robot.
// ===========================================================================
package com.bhoomibot.os.feature.live

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bhoomibot.os.connection.model.ConnectionConfig
import com.bhoomibot.os.connection.model.RobotCommand
import com.bhoomibot.os.connection.model.VideoQuality
import com.bhoomibot.os.connection.repository.LiveLinkRepository
import com.bhoomibot.os.connection.provideLiveLinkRepository
import com.bhoomibot.os.connection.transport.AndroidFrameDecoder
import com.bhoomibot.os.connection.transport.FrameDecoder
import com.bhoomibot.os.data.LiveLinkPreferencesStore
import com.bhoomibot.os.model.DeviceRole
import com.bhoomibot.os.model.DriveCommand
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Operator-side live link. Connects as [DeviceRole.OPERATOR], decodes the
 * inbound jpeg frames into a Compose [ImageBitmap], and forwards drive/aux
 * commands back to the robot. The [FrameDecoder] is injected so this can be
 * unit-tested without Android's BitmapFactory.
 */
// Extends AndroidViewModel (not plain ViewModel) because it needs the Application
// context. viewModel() in the composable uses AndroidViewModelFactory, which can
// only call a constructor whose FIRST param is Application. The extra params below
// have defaults so that factory path still works; the defaults are also the seam
// for tests to inject a fake repository / a decoder that doesn't need Android's
// BitmapFactory.
class OperatorLiveViewModel(
    application: Application,
    internal var repository: LiveLinkRepository = provideLiveLinkRepository(application),
    private val decoder: FrameDecoder = AndroidFrameDecoder()
) : AndroidViewModel(application) {

    // _uiState is the private, mutable source of truth; uiState is the read-only
    // view the screen observes. Screen state is only ever changed through this.
    private val _uiState = MutableStateFlow(OperatorLiveUiState())
    val uiState: StateFlow<OperatorLiveUiState> = _uiState.asStateFlow()

    // The connection settings, built once from saved preferences (see init).
    private var config: ConnectionConfig? = null

    init {
        // First coroutine: read saved settings once (first() takes a single value
        // from the preferences flow), build the OPERATOR connection config, and
        // open the link. valueOf can throw if a stored quality string is invalid,
        // so runCatching falls back to MEDIUM.
        viewModelScope.launch {
            val prefs = LiveLinkPreferencesStore.preferences(application).first()
            config = ConnectionConfig(
                serverUrl = prefs.serverUrl,
                robotId = prefs.robotId,
                sessionCode = prefs.sessionCode,
                role = DeviceRole.OPERATOR,
                autoReconnect = prefs.autoReconnect,
                videoFps = prefs.videoFps,
                videoQuality = runCatching { VideoQuality.valueOf(prefs.videoQuality) }
                    .getOrDefault(VideoQuality.MEDIUM)
            )
            repository.connect(config!!)
        }
        // Each of these coroutines mirrors one repository flow into the single
        // UiState. `it` is the newly emitted value; `s` is the current state we
        // copy() so only that one field changes (data classes are immutable).
        viewModelScope.launch { repository.connectionState.collect { _uiState.update { s -> s.copy(connectionState = it) } } }
        viewModelScope.launch { repository.peerStatus.collect { _uiState.update { s -> s.copy(peerStatus = it) } } }
        viewModelScope.launch { repository.telemetry.collect { _uiState.update { s -> s.copy(telemetry = it) } } }
        // Frames arrive as raw jpeg bytes; decode each into an ImageBitmap the
        // Compose Image() can draw. Decoding goes through the injected decoder.
        viewModelScope.launch {
            repository.frames.collect { frame ->
                val bmp = decoder.decode(frame.jpeg)
                _uiState.update { s -> s.copy(frame = bmp) }
            }
        }
    }

    // --- Outbound commands: wrap the intent in a RobotCommand and send it to
    // the robot. speedPercent defaults to 0 so a bare directional tap is valid.
    fun sendDrive(command: DriveCommand, speedPercent: Int = 0) {
        repository.sendCommand(RobotCommand(drive = command, speedPercent = speedPercent))
    }

    // E-stop sets the explicit flag AND the EMERGENCY_STOP drive value so the
    // robot halts even if it only reads one of the two.
    fun sendEmergencyStop() {
        repository.sendCommand(RobotCommand(emergencyStop = true, drive = DriveCommand.EMERGENCY_STOP))
    }

    // Auxiliary toggles (power take-off, lights). Not wired to the current UI
    // but available for the options/aux controls.
    fun togglePto(on: Boolean) {
        repository.sendCommand(RobotCommand(pto = on))
    }

    fun toggleLights(on: Boolean) {
        repository.sendCommand(RobotCommand(lights = on))
    }

    // Called when the screen (and its ViewModel) goes away — close the socket so
    // we don't leak the connection.
    override fun onCleared() {
        super.onCleared()
        repository.disconnect()
    }
}
