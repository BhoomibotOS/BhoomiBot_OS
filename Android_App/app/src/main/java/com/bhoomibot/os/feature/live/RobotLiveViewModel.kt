package com.bhoomibot.os.feature.live

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bhoomibot.os.connection.model.ConnectionConfig
import com.bhoomibot.os.connection.model.VideoQuality
import com.bhoomibot.os.connection.model.toTelemetry
import com.bhoomibot.os.connection.repository.LiveLinkRepository
import com.bhoomibot.os.connection.provideLiveLinkRepository
import com.bhoomibot.os.connection.transport.LiveConnectionState
import com.bhoomibot.os.connection.transport.local.LocalHubManager
import com.bhoomibot.os.data.LiveLinkPreferencesStore
import com.bhoomibot.os.feature.autonomous.AutonomyManager
import com.bhoomibot.os.feature.connection.PhoneNetworkMode
import com.bhoomibot.os.feature.autonomous.ai.PerceptionEngine
import com.bhoomibot.os.model.DeviceRole
import com.bhoomibot.os.model.MockRobotData
import com.bhoomibot.os.repository.RobotRepository
import com.bhoomibot.os.repository.provideRobotRepository
import com.bhoomibot.os.service.BhoomiBotService
import android.graphics.Bitmap
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
 * RobotLiveViewModel: Manages the UI-side of the Robot's live presence.
 * AI-Fix: Actual command forwarding and camera analysis has been moved to 
 * BhoomiBotService to ensure operations continue when screen is locked.
 */
class RobotLiveViewModel(application: Application) : AndroidViewModel(application) {
    internal var repository: LiveLinkRepository = provideLiveLinkRepository(application)
    private val robotRepository: RobotRepository = provideRobotRepository(application)
    private val perceptionEngine = AutonomyManager.getPerceptionEngine(application)

    private val _uiState = MutableStateFlow(RobotLiveUiState())
    val uiState: StateFlow<RobotLiveUiState> = _uiState.asStateFlow()

    private var config: ConnectionConfig? = null
    private var telemetryJob: Job? = null
    private val connectivityManager = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    private var savedVideoFps = 12
    private var savedVideoQuality = VideoQuality.MEDIUM
    private var isRearCamera = true
    
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = updateNetworkProfile()
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) = updateNetworkProfile()
        override fun onLost(network: Network) = updateNetworkProfile()
    }

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        _uiState.update { it.copy(error = throwable.message ?: "Unexpected error during live link") }
    }

    init {
        // AI Tracking (UI only displays the results from the background engine)
        viewModelScope.launch {
            perceptionEngine.aiStatus.collect { status ->
                _uiState.update { it.copy(aiStatus = status) }
            }
        }
        viewModelScope.launch {
            perceptionEngine.steeringOffset.collect { offset ->
                _uiState.update { it.copy(aiSteeringOffset = offset) }
            }
        }
        viewModelScope.launch {
            perceptionEngine.detectedWeeds.collect { objects ->
                _uiState.update { it.copy(detectedObjects = objects) }
            }
        }

        viewModelScope.launch(exceptionHandler) {
            val prefs = LiveLinkPreferencesStore.preferences(application).first()
            val role = com.bhoomibot.os.data.DevicePreferences.role(application).first() ?: DeviceRole.ROBOT
            
            config = ConnectionConfig(
                serverUrl = prefs.serverUrl,
                robotId = prefs.robotId,
                sessionCode = prefs.sessionCode,
                role = role,
                autoReconnect = prefs.autoReconnect,
                videoFps = prefs.videoFps,
                videoQuality = runCatching { VideoQuality.valueOf(prefs.videoQuality) }.getOrDefault(VideoQuality.MEDIUM)
            )
            savedVideoFps = config!!.videoFps
            savedVideoQuality = config!!.videoQuality
            val mode = runCatching { PhoneNetworkMode.valueOf(prefs.networkMode) }.getOrDefault(PhoneNetworkMode.INTERNET)
            updateNetworkProfile()
            _uiState.update {
                it.copy(
                    activeRole = role,
                    activeRobotId = config!!.robotId,
                    activeSession = config!!.sessionCode,
                    networkMode = mode
                )
            }
            repository.connect(config!!)
            
            // AI-Fix: Auto-start broadcast as soon as config is ready
            startBroadcast()
            
            // Sync status to operator immediately
            startTelemetry()
        }

        viewModelScope.launch(exceptionHandler) { repository.connectionState.collect { _uiState.update { s -> s.copy(connectionState = it) } } }
        viewModelScope.launch(exceptionHandler) { repository.connectionError.collect { _uiState.update { s -> s.copy(error = it) } } }
        viewModelScope.launch(exceptionHandler) { repository.peerStatus.collect { _uiState.update { s -> s.copy(peerStatus = it) } } }

        // AI-Fix: We no longer listen for commands here. 
        // BhoomiBotService is now the permanent listener for commands.

        connectivityManager.registerDefaultNetworkCallback(networkCallback)
    }

    fun startBroadcast() {
        val cfg = config ?: return
        if (_uiState.value.networkMode == PhoneNetworkMode.HOTSPOT) {
            LocalHubManager.startServer(getApplication())
        }
        
        repository.connect(cfg)
        _uiState.update { it.copy(isBroadcasting = true) }
        startTelemetry()
        
        val intent = Intent(getApplication(), BhoomiBotService::class.java).apply {
            action = BhoomiBotService.ACTION_START_BROADCAST
            putExtra("EXTRA_QUALITY", _uiState.value.videoQuality.name)
            putExtra("EXTRA_USE_REAR", isRearCamera)
        }
        getApplication<Application>().startService(intent)

        viewModelScope.launch {
            robotRepository.sendDriveCommand(com.bhoomibot.os.model.DriveCommand.STOP)
        }
    }

    fun flipCamera() {
        isRearCamera = !isRearCamera
        // Update local state if not broadcasting, so next start picks it up
        if (_uiState.value.isBroadcasting) {
            val intent = Intent(getApplication(), BhoomiBotService::class.java).apply {
                action = BhoomiBotService.ACTION_SWITCH_CAMERA
                putExtra("EXTRA_USE_REAR", isRearCamera)
            }
            getApplication<Application>().startService(intent)
        }
    }

    fun stopBroadcast() {
        _uiState.update { it.copy(isBroadcasting = false, lastCommand = null) }
        val intent = Intent(getApplication(), BhoomiBotService::class.java).apply {
            action = BhoomiBotService.ACTION_STOP_BROADCAST
        }
        getApplication<Application>().startService(intent)
    }

    fun retry() {
        _uiState.update { it.copy(error = null) }
        val cfg = config ?: return
        repository.disconnect()
        if (_uiState.value.isBroadcasting) startBroadcast() else repository.connect(cfg)
    }

    fun publishFrame(jpeg: ByteArray) {
        // Service handles this now
    }

    fun processVisionFrame(bitmap: Bitmap) {
    }

    private fun startTelemetry() {
        if (telemetryJob?.isActive == true) return
        
        telemetryJob = viewModelScope.launch {
            while (isActive) {
                val hardwareOnline = robotRepository.isConnected.value
                val realStatus = MockRobotData.robotStatus.copy(
                    isOnline = hardwareOnline
                )
                repository.publishTelemetry(realStatus.toTelemetry())
                delay(500)
            }
        }
    }

    private fun updateNetworkProfile() {
        val network = connectivityManager.activeNetwork
        val caps = network?.let { connectivityManager.getNetworkCapabilities(it) }
        val isHotspotMode = _uiState.value.networkMode == PhoneNetworkMode.HOTSPOT

        _uiState.update {
            it.copy(
                isConfigurationReady = config != null,
                // CLOUDFLARE OPTIMIZATION: Use the saved FPS and Quality without caps.
                // We no longer downgrade to 720p/12fps on internet because binary relay handles it.
                videoFps = savedVideoFps,
                videoQuality = savedVideoQuality,
                isConstrainedNetwork = !isHotspotMode && (caps == null || !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED))
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        telemetryJob?.cancel()
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }
}
