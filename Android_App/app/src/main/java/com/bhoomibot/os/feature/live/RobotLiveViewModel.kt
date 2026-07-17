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
import com.bhoomibot.os.data.LiveLinkPreferencesStore
import com.bhoomibot.os.model.DeviceRole
import com.bhoomibot.os.model.MockRobotData
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

    init {
        viewModelScope.launch {
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
        }
        // Mirror repository flows into UiState (same pattern as the operator VM).
        viewModelScope.launch { repository.connectionState.collect { _uiState.update { s -> s.copy(connectionState = it) } } }
        viewModelScope.launch { repository.peerStatus.collect { _uiState.update { s -> s.copy(peerStatus = it) } } }
        // Inbound commands from the operator (the robot would forward these to the VCU).
        viewModelScope.launch { repository.incomingCommands.collect { _uiState.update { s -> s.copy(lastCommand = it) } } }
        connectivityManager.registerDefaultNetworkCallback(networkCallback)
    }

    // START: bail if settings haven't loaded yet, otherwise open the link, mark
    // broadcasting, and begin the telemetry heartbeat. Frames start flowing once
    // the screen's CameraX pipeline binds (it keys off isBroadcasting).
    fun startBroadcast() {
        val cfg = config ?: return
        repository.connect(cfg)
        _uiState.update { it.copy(isBroadcasting = true) }
        startTelemetry()
    }

    // STOP: end the telemetry loop, close the link, and clear broadcast state.
    // lastCommand is cleared so a stale operator command isn't shown next time.
    fun stopBroadcast() {
        telemetryJob?.cancel()
        telemetryJob = null
        repository.disconnect()
        _uiState.update { it.copy(isBroadcasting = false, lastCommand = null) }
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
