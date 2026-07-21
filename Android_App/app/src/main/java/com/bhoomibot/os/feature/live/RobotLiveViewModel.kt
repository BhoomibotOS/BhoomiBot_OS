// ===========================================================================
// RobotLiveViewModel.kt
// ---------------------------------------------------------------------------
// Backs RobotLiveScreen. Owns the robot's live-link session and broadcast
// state. It:
//   1. loads settings and builds the ROBOT connection config,
//   2. on START connects the link, publishes camera frames (fed from the
//      screen's CameraX analyzer) and a 1 Hz telemetry snapshot,
//   3. surfaces the last command the operator sent, and
//   4. watches the network and, on a constrained (metered/cellular) link,
//      lowers fps/quality to save data.
// ===========================================================================
package com.bhoomibot.os.feature.live

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bhoomibot.os.connection.model.ConnectionConfig
import com.bhoomibot.os.connection.model.RobotCommand
import com.bhoomibot.os.connection.model.VideoQuality
import com.bhoomibot.os.connection.model.toTelemetry
import com.bhoomibot.os.connection.repository.LiveLinkRepository
import com.bhoomibot.os.connection.provideLiveLinkRepository
import com.bhoomibot.os.connection.transport.LiveConnectionState
import com.bhoomibot.os.data.LiveLinkPreferencesStore
import com.bhoomibot.os.model.DeviceRole
import com.bhoomibot.os.model.MockRobotData
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Robot-side live link. Connects as [DeviceRole.ROBOT], streams the camera
 * (frames pushed from the CameraX analyzer via [publishFrame]) + a 1 Hz telemetry
 * snapshot, and surfaces any command the operator sends back.
 */
// AndroidViewModel because it needs the Application context (network service +
// preferences). The repository is a plain field rather than a constructor param:
// viewModel()'s AndroidViewModelFactory only calls a (Application) constructor,
// so there's no way to inject via the constructor here.
class RobotLiveViewModel(application: Application) : AndroidViewModel(application) {
    internal var repository: LiveLinkRepository = provideLiveLinkRepository(application)

    // Private mutable state + read-only view the screen collects (see UiState file).
    private val _uiState = MutableStateFlow(RobotLiveUiState())
    val uiState: StateFlow<RobotLiveUiState> = _uiState.asStateFlow()

    private var config: ConnectionConfig? = null
    // The repeating coroutine that emits telemetry once a second; null when stopped.
    private var telemetryJob: Job? = null
    private val connectivityManager = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    // The user's chosen ("full quality") settings. We remember them so we can
    // restore them when the link becomes unconstrained again, after temporarily
    // downgrading on a metered/cellular network.
    private var savedVideoFps = 12
    private var savedVideoQuality = VideoQuality.MEDIUM
    // Fires whenever the active network changes; each event re-evaluates whether
    // we're on a constrained link and adjusts video accordingly.
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = updateNetworkProfile()
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) = updateNetworkProfile()
        override fun onLost(network: Network) = updateNetworkProfile()
    }

    // Any unexpected exception in the coroutines below is caught here and shown as
    // an error message instead of crashing the app.
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        _uiState.update { it.copy(error = throwable.message ?: "Unexpected error during live link") }
    }

    init {
        viewModelScope.launch(exceptionHandler) {
            val prefs = LiveLinkPreferencesStore.preferences(application).first()
            config = ConnectionConfig(
                serverUrl = prefs.serverUrl,
                robotId = prefs.robotId,
                sessionCode = prefs.sessionCode,
                role = DeviceRole.ROBOT,
                autoReconnect = prefs.autoReconnect,
                videoFps = prefs.videoFps,
                videoQuality = runCatching { VideoQuality.valueOf(prefs.videoQuality) }
                    .getOrDefault(VideoQuality.MEDIUM)
            )
            savedVideoFps = config!!.videoFps
            savedVideoQuality = config!!.videoQuality
            // Compute the initial network profile now that we have the baseline
            // settings; this also flips isConfigurationReady true.
            updateNetworkProfile()
            // Surface the role + meet-keys this phone is actually using, so a
            // mismatch with the other phone shows up instead of a silent
            // "connected, but no video".
            _uiState.update {
                it.copy(
                    activeRole = DeviceRole.ROBOT,
                    activeRobotId = config!!.robotId,
                    activeSession = config!!.sessionCode
                )
            }
            // Open the socket as soon as settings load (mirrors the operator VM), so
            // "both connected" no longer waits on a manual Start Broadcast tap. The
            // camera + telemetry still only flow once startBroadcast() runs.
            repository.connect(config!!)
        }
        // Mirror repository flows into UiState (same pattern as the operator VM).
        viewModelScope.launch(exceptionHandler) { repository.connectionState.collect { _uiState.update { s -> s.copy(connectionState = it) } } }
        viewModelScope.launch(exceptionHandler) { repository.connectionError.collect { _uiState.update { s -> s.copy(error = it) } } }
        viewModelScope.launch(exceptionHandler) { repository.peerStatus.collect { _uiState.update { s -> s.copy(peerStatus = it) } } }
        // Inbound commands from the operator (the robot would forward these to the VCU).
        viewModelScope.launch(exceptionHandler) { repository.incomingCommands.collect { cmd ->
            // A non-null liveCamera field is the operator's remote "live camera"
            // switch: true -> start broadcasting, false -> stop. Stopping keeps the
            // relay connection alive; only the frame/telemetry stream halts.
            if (cmd.liveCamera != null) {
                if (cmd.liveCamera == true) startBroadcast() else stopBroadcast()
            }
            // Pure broadcast toggles carry no drive intent, so don't surface them as
            // the "last command" the operator sent.
            if (cmd.liveCamera == null) {
                _uiState.update { s -> s.copy(lastCommand = cmd) }
            }
        } }
        connectivityManager.registerDefaultNetworkCallback(networkCallback)
    }

    // START: bail if settings haven't loaded yet, otherwise make sure the link is
    // up (only reconnecting if it isn't already), mark broadcasting, and begin the
    // telemetry heartbeat. Frames start flowing once the screen's CameraX pipeline
    // binds (it keys off isBroadcasting).
    fun startBroadcast() {
        val cfg = config
        if (cfg == null) {
            android.util.Log.e("BhoomiBotRelay", "[GUI] RobotLiveViewModel.startBroadcast() — config is null, not connecting")
            return
        }
        android.util.Log.d(
            "BhoomiBotRelay",
            "[GUI] RobotLiveViewModel.startBroadcast() — connecting: url='${cfg.serverUrl}' " +
                "role=${cfg.role} robotId='${cfg.robotId}' session='${cfg.sessionCode}'"
        )
        // Connect only if we're not already connected — the link is opened once in
        // init and must stay alive across start/stop broadcasts. Reconnecting here
        // would briefly drop the operator's session.
        if (_uiState.value.connectionState != LiveConnectionState.CONNECTED) {
            repository.connect(cfg)
        }
        _uiState.update { it.copy(isBroadcasting = true) }
        startTelemetry()
    }

    // STOP: end the telemetry loop and stop the camera/telemetry stream, but KEEP
    // the relay connection alive (the operator's "live camera" switch expects the
    // link to persist — only the broadcast stops). lastCommand is cleared so a
    // stale operator command isn't shown next time.
    fun stopBroadcast() {
        telemetryJob?.cancel()
        telemetryJob = null
        _uiState.update { it.copy(isBroadcasting = false, lastCommand = null) }
    }

    // Re-establish the link after an error (or a manual retry from the UI).
    fun retry() {
        _uiState.update { it.copy(error = null) }
        val cfg = config ?: return
        repository.disconnect()
        if (_uiState.value.isBroadcasting) {
            startBroadcast()
        } else {
            repository.connect(cfg)
        }
    }

    /** Called by the CameraX analyzer with each compressed jpeg frame. */
    fun publishFrame(jpeg: ByteArray) {
        repository.publishFrame(jpeg)
        _uiState.update { it.copy(framesSent = it.framesSent + 1) }
    }

    // Telemetry heartbeat: every second, publish a fresh snapshot. Currently
    // sourced from MockRobotData (a real robot would read live sensors here).
    // The loop lives while the coroutine isActive; stopBroadcast()/onCleared()
    // cancel the job to end it.
    private fun startTelemetry() {
        telemetryJob?.cancel()
        telemetryJob = viewModelScope.launch {
            while (isActive) {
                repository.publishTelemetry(MockRobotData.robotStatus.toTelemetry())
                delay(1000)
            }
        }
    }

    /** Adapts outgoing video immediately when this robot switches to a constrained mobile link. */
    private fun updateNetworkProfile() {
        // Inspect the active network's capabilities. Any of these can be null if
        // there's no connectivity, so we treat "no caps" as offline/constrained.
        val network = connectivityManager.activeNetwork
        val caps = network?.let { connectivityManager.getNetworkCapabilities(it) }
        val onWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val onCellular = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        // NOT_METERED means the OS considers this link free of data charges.
        val unmetered = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) == true
        // "Constrained" = offline or any metered link -> we should conserve data.
        val constrained = caps == null || !unmetered
        val label = when {
            caps == null -> "Offline"
            onWifi && unmetered -> "Wi-Fi"
            onWifi -> "Metered Wi-Fi"
            onCellular -> "Mobile data"
            else -> "Internet"
        }
        // On a constrained link, cap fps at 8 and force LOW quality; otherwise
        // restore the user's saved settings. The screen's CameraX analyzer reads
        // these values live, so the change takes effect on the next frame.
        _uiState.update {
            it.copy(
                isConfigurationReady = config != null,
                videoFps = if (constrained) minOf(savedVideoFps, 8) else savedVideoFps,
                videoQuality = if (constrained) VideoQuality.LOW else savedVideoQuality,
                networkLabel = label,
                isConstrainedNetwork = constrained
            )
        }
    }

    // Tear everything down when the screen goes away: stop telemetry, stop
    // listening for network changes, and close the link.
    override fun onCleared() {
        super.onCleared()
        telemetryJob?.cancel()
        connectivityManager.unregisterNetworkCallback(networkCallback)
        repository.disconnect()
    }
}
