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
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
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
class OperatorLiveViewModel(application: Application) : AndroidViewModel(application) {

    // Injected dependencies are FIELDS (not constructor params) so the default
    // ViewModel factory used by viewModel() can build this with the sole
    // (Application) constructor it requires. A Kotlin class whose primary
    // constructor has defaulted params does NOT synthesize a (Application)-only
    // constructor, so viewModel() would reflectively call getConstructor(Application)
    // and throw NoSuchMethodException — crashing the operator screen on open. This
    // is the same pattern as RobotLiveViewModel / ManualViewModel (see their notes).
    internal var repository: LiveLinkRepository = provideLiveLinkRepository(application)
    private val decoder: FrameDecoder = AndroidFrameDecoder()

    // _uiState is the private, mutable source of truth; uiState is the read-only
    // view the screen observes. Screen state is only ever changed through this.
    private val _uiState = MutableStateFlow(OperatorLiveUiState())
    val uiState: StateFlow<OperatorLiveUiState> = _uiState.asStateFlow()

    // The connection settings, built once from saved preferences (see init).
    private var config: ConnectionConfig? = null

    // Any unexpected exception in the coroutines below is caught here and shown to
    // the user as an error message instead of crashing the whole app (which would
    // otherwise look like "the app minimized"). This is the safety net that lets
    // the operator connect without the app dying on an unforeseen error.
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        _uiState.update { it.copy(error = throwable.message ?: "Unexpected error during live link") }
    }

    init {
        // First coroutine: read saved settings once (first() takes a single value
        // from the preferences flow), build the OPERATOR connection config, and
        // open the link. valueOf can throw if a stored quality string is invalid,
        // so runCatching falls back to MEDIUM.
        viewModelScope.launch(exceptionHandler) {
            val prefs = LiveLinkPreferencesStore.preferences(application).first()
            val cfg = ConnectionConfig(
                serverUrl = prefs.serverUrl,
                robotId = prefs.robotId,
                sessionCode = prefs.sessionCode,
                role = DeviceRole.OPERATOR,
                autoReconnect = prefs.autoReconnect,
                videoFps = prefs.videoFps,
                videoQuality = runCatching { VideoQuality.valueOf(prefs.videoQuality) }
                    .getOrDefault(VideoQuality.MEDIUM)
            )
            config = cfg
            // Surface the role + meet-keys this phone is actually using, so a
            // mismatch with the other phone shows up instead of a silent
            // "connected, but no video".
            _uiState.update {
                it.copy(
                    activeRole = cfg.role,
                    activeRobotId = cfg.robotId,
                    activeSession = cfg.sessionCode
                )
            }
            android.util.Log.d(
                "BhoomiBotRelay",
                "[GUI] OperatorLiveViewModel init — connecting: url='${cfg.serverUrl}' " +
                    "role=${cfg.role} robotId='${cfg.robotId}' session='${cfg.sessionCode}'"
            )
            repository.connect(cfg)
        }
        // Each of these coroutines mirrors one repository flow into the single
        // UiState. `it` is the newly emitted value; `s` is the current state we
        // copy() so only that one field changes (data classes are immutable).
        viewModelScope.launch(exceptionHandler) { repository.connectionState.collect { _uiState.update { s -> s.copy(connectionState = it) } } }
        viewModelScope.launch(exceptionHandler) { repository.connectionError.collect { _uiState.update { s -> s.copy(error = it) } } }
        viewModelScope.launch(exceptionHandler) { repository.peerStatus.collect { _uiState.update { s -> s.copy(peerStatus = it) } } }
        viewModelScope.launch(exceptionHandler) { repository.telemetry.collect { _uiState.update { s -> s.copy(telemetry = it) } } }
        // Frames arrive as raw jpeg bytes; decode each into an ImageBitmap the
        // Compose Image() can draw. Decoding is HEAVY (BitmapFactory), so it MUST
        // run off the main thread — decoding on the UI thread for every frame
        // (12–30 fps) blocks the UI and causes an ANR that looks like "the app
        // minimized". Dispatchers.Default keeps the UI responsive and stable.
        viewModelScope.launch(Dispatchers.Default + exceptionHandler) {
            repository.frames.collect { frame ->
                val bmp = runCatching { decoder.decode(frame.jpeg) }.getOrNull()
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

    // Remote "live camera" switch: tells the robot to START (true) or STOP (false)
    // broadcasting its camera. Stopping keeps the relay connection alive.
    fun sendLiveCamera(on: Boolean) {
        repository.sendCommand(RobotCommand(liveCamera = on))
    }

    // Operator's "live camera" toggle on this screen: updates the local view
    // state and remotely tells the robot to start/stop broadcasting.
    fun setLiveCameraEnabled(on: Boolean) {
        _uiState.update { it.copy(liveCameraEnabled = on) }
        sendLiveCamera(on)
    }

    // Auxiliary toggles (power take-off, lights). Not wired to the current UI
    // but available for the options/aux controls.
    fun togglePto(on: Boolean) {
        repository.sendCommand(RobotCommand(pto = on))
    }

    fun toggleLights(on: Boolean) {
        repository.sendCommand(RobotCommand(lights = on))
    }

    // Re-establish the link after an error (or a manual retry from the UI). Clears
    // the error message, then disconnect + reconnect with the same config.
    fun retry() {
        _uiState.update { it.copy(error = null) }
        val cfg = config ?: return
        repository.disconnect()
        repository.connect(cfg)
    }

    // Called when the screen (and its ViewModel) goes away — close the socket so
    // we don't leak the connection.
    override fun onCleared() {
        super.onCleared()
        repository.disconnect()
    }
}
