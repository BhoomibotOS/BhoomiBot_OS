package com.bhoomibot.os.feature.autonomous

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bhoomibot.os.connection.repository.LiveLinkRepositoryProvider
import com.bhoomibot.os.model.DriveCommand
import com.bhoomibot.os.repository.provideRobotRepository
import android.media.AudioManager
import android.media.ToneGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class AutonomyUiState(
    val state: AutonomyState = AutonomyState.IDLE,
    val errorMessage: String? = null,
    val activeAlert: Pair<String, String>? = null
)

class AutonomyViewModel(application: Application) : AndroidViewModel(application) {

    private val stateMachine = AutonomyManager.stateMachine
    private val robotRepository = provideRobotRepository(application)
    private val liveLinkRepository = LiveLinkRepositoryProvider.get(application)
    private val toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
    
    private val _uiState = MutableStateFlow(AutonomyUiState())
    val uiState: StateFlow<AutonomyUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            stateMachine.state.collect { state ->
                _uiState.value = _uiState.value.copy(state = state)
            }
        }
        
        viewModelScope.launch {
            liveLinkRepository.alerts.collectLatest { alert ->
                _uiState.value = _uiState.value.copy(activeAlert = alert)
                // Ring the operator (AI-002)
                toneGenerator.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 1000)
            }
        }
    }

    fun onEmergencyStop() {
        stateMachine.transitionTo(AutonomyState.EMERGENCY_STOP)
        robotRepository.sendDriveCommand(DriveCommand.EMERGENCY_STOP)
    }

    fun clearError() {
        if (_uiState.value.state == AutonomyState.ERROR || _uiState.value.state == AutonomyState.EMERGENCY_STOP) {
            stateMachine.reset()
            _uiState.value = _uiState.value.copy(errorMessage = null, activeAlert = null)
        }
    }

    fun dismissAlert() {
        _uiState.value = _uiState.value.copy(activeAlert = null)
    }
}
