// ===========================================================================
// OperatorLiveViewModel.kt
// ---------------------------------------------------------------------------
// Backs OperatorLiveScreen. Owns the operator's live-link session and the
// screen's state.
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
import com.bhoomibot.os.feature.autonomous.AutonomyManager
import com.bhoomibot.os.feature.connection.PhoneNetworkMode
import com.bhoomibot.os.service.BhoomiBotService
import com.bhoomibot.os.model.DeviceRole
import com.bhoomibot.os.model.DriveCommand
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OperatorLiveViewModel(application: Application) : AndroidViewModel(application) {

    internal var repository: LiveLinkRepository = provideLiveLinkRepository(application)
    private val decoder: FrameDecoder = AndroidFrameDecoder()
    private val perceptionEngine = AutonomyManager.getPerceptionEngine(application)

    private val _uiState = MutableStateFlow(OperatorLiveUiState())
    val uiState: StateFlow<OperatorLiveUiState> = _uiState.asStateFlow()

    private var config: ConnectionConfig? = null

    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        _uiState.update { it.copy(error = throwable.message ?: "Unexpected error during live link") }
    }

    init {
        // AI-Fix: Background Service is only for the Robot Phone. 
        // Removing it from Operator to prevent connection flickering.

        // Collect AI status from PerceptionEngine
        viewModelScope.launch {
            perceptionEngine.aiStatus.collect { status ->
                _uiState.update { it.copy(aiStatus = status) }
            }
        }
        // Collect Steering Offset from PerceptionEngine
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
            val cfg = ConnectionConfig(
                serverUrl = prefs.serverUrl,
                robotId = prefs.robotId,
                sessionCode = prefs.sessionCode,
                role = DeviceRole.OPERATOR,
                autoReconnect = prefs.autoReconnect,
                videoFps = prefs.videoFps,
                videoQuality = runCatching { VideoQuality.valueOf(prefs.videoQuality) }.getOrDefault(VideoQuality.MEDIUM)
            )
            config = cfg
            val mode = runCatching { PhoneNetworkMode.valueOf(prefs.networkMode) }.getOrDefault(PhoneNetworkMode.INTERNET)
            _uiState.update {
                it.copy(
                    activeRole = cfg.role,
                    activeRobotId = cfg.robotId,
                    activeSession = cfg.sessionCode,
                    networkMode = mode
                )
            }
            repository.connect(cfg)
        }

        viewModelScope.launch(exceptionHandler) { repository.connectionState.collect { _uiState.update { s -> s.copy(connectionState = it) } } }
        viewModelScope.launch(exceptionHandler) { repository.connectionError.collect { _uiState.update { s -> s.copy(error = it) } } }
        viewModelScope.launch(exceptionHandler) { repository.peerStatus.collect { _uiState.update { s -> s.copy(peerStatus = it) } } }
        viewModelScope.launch(exceptionHandler) { repository.telemetry.collect { _uiState.update { s -> s.copy(telemetry = it) } } }

        viewModelScope.launch(Dispatchers.Default + exceptionHandler) {
            repository.frames.collectLatest { frame: com.bhoomibot.os.connection.model.LiveFrame ->
                val bmp = runCatching { decoder.decode(frame.jpeg) }.getOrNull()
                _uiState.update { s -> s.copy(frame = bmp) }
            }
        }
    }

    fun sendDrive(command: DriveCommand, speedPercent: Int = 0) {
        repository.sendCommand(RobotCommand(
            drive = command, 
            speedPercent = speedPercent,
            useRearCamera = _uiState.value.useRearCamera
        ))
    }

    fun sendEmergencyStop() {
        repository.sendCommand(RobotCommand(
            emergencyStop = true, 
            drive = DriveCommand.EMERGENCY_STOP,
            useRearCamera = _uiState.value.useRearCamera
        ))
    }

    fun sendLiveCamera(on: Boolean) {
        repository.sendCommand(RobotCommand(liveCamera = on))
    }

    fun setLiveCameraEnabled(on: Boolean) {
        _uiState.update { it.copy(liveCameraEnabled = on) }
        sendLiveCamera(on)
    }

    fun setUseRearCamera(useRear: Boolean) {
        _uiState.update { it.copy(useRearCamera = useRear) }
        repository.sendCommand(RobotCommand(
            liveCamera = _uiState.value.liveCameraEnabled,
            useRearCamera = useRear
        ))
    }

    fun togglePto(on: Boolean) {
        repository.sendCommand(RobotCommand(pto = on))
    }

    fun toggleLights(on: Boolean) {
        repository.sendCommand(RobotCommand(lights = on))
    }

    fun retry() {
        _uiState.update { it.copy(error = null) }
        val cfg = config ?: return
        repository.disconnect()
        repository.connect(cfg)
    }

    override fun onCleared() {
        super.onCleared()
        // We no longer call repository.disconnect() here to keep the video active
    }
}
