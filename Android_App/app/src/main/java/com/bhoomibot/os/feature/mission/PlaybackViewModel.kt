package com.bhoomibot.os.feature.mission

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bhoomibot.os.feature.autonomous.skills.models.ExperienceDelta
import com.bhoomibot.os.feature.autonomous.skills.library.SkillLibrary
import com.bhoomibot.os.model.DriveCommand
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PlaybackUiState(
    val currentStepIndex: Int = -1,
    val totalSteps: Int = 0,
    val isIntervened: Boolean = false,
    val activeSkillId: String? = null,
    val isVirtualMode: Boolean = false,
    val virtualRobotPose: com.bhoomibot.os.feature.autonomous.core.model.RobotPose? = null,
    val currentActionType: com.bhoomibot.os.feature.autonomous.skills.models.ActionType? = null
)

class PlaybackViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(PlaybackUiState())
    val uiState: StateFlow<PlaybackUiState> = _uiState.asStateFlow()

    private var validationJob: kotlinx.coroutines.Job? = null

    fun setVirtualMode(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isVirtualMode = enabled)
    }

    /**
     * Simulation Engine: Moves the ghost robot along the path.
     */
    fun startVirtualValidation(steps: List<com.bhoomibot.os.feature.autonomous.skills.models.SkillStep>) {
        validationJob?.cancel()
        validationJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isVirtualMode = true, 
                totalSteps = steps.size,
                currentStepIndex = 0
            )
            
            val sortedSteps = steps.sortedBy { it.sequence }
            
            sortedSteps.forEachIndexed { index, step ->
                _uiState.value = _uiState.value.copy(
                    currentStepIndex = index,
                    currentActionType = step.actionType
                )
                
                if (step.latitude != null && step.longitude != null) {
                    val heading = if (index < sortedSteps.size - 1) {
                        val nextStep = sortedSteps.drop(index + 1).firstOrNull { it.latitude != null }
                        if (nextStep != null) {
                            calculateHeading(step.latitude!!, step.longitude!!, nextStep.latitude!!, nextStep.longitude!!)
                        } else 0.0
                    } else 0.0
                    
                    runVirtualStep(step.latitude!!, step.longitude!!, heading)
                }
                
                // Wait for step simulation
                val delayMs = when(step.actionType) {
                    com.bhoomibot.os.feature.autonomous.skills.models.ActionType.WAIT -> 1000L
                    else -> 500L
                }
                delay(delayMs)
            }
            
            _uiState.value = _uiState.value.copy(
                isVirtualMode = false,
                currentStepIndex = -1,
                currentActionType = null
            )
        }
    }

    private fun calculateHeading(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLon = Math.toRadians(lon2 - lon1)
        val y = Math.sin(dLon) * Math.cos(Math.toRadians(lat2))
        val x = Math.cos(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2)) -
                Math.sin(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(dLon)
        val brng = Math.toDegrees(Math.atan2(y, x))
        return (brng + 360) % 360
    }

    fun startPhysicalReplay(skill: com.bhoomibot.os.feature.autonomous.skills.models.DemonstratedSkill) {
        // Implementation for physical replay via AutonomyManager
        // For now, this bridges to the playback engine if possible or triggers autonomy
    }

    fun stopVirtualValidation() {
        validationJob?.cancel()
        _uiState.value = _uiState.value.copy(
            isVirtualMode = false,
            currentStepIndex = -1,
            virtualRobotPose = null,
            currentActionType = null
        )
    }

    private fun runVirtualStep(lat: Double, lon: Double, heading: Double = 0.0) {
        _uiState.value = _uiState.value.copy(
            virtualRobotPose = com.bhoomibot.os.feature.autonomous.core.model.RobotPose(
                latitude = lat,
                longitude = lon,
                heading = heading,
                speedMps = 0f,
                accuracyMeters = 0f
            )
        )
    }

    fun onManualIntervention(command: DriveCommand) {
        if (!_uiState.value.isIntervened) {
            _uiState.value = _uiState.value.copy(isIntervened = true)
        }
    }

    fun saveCorrection(lat: Double, lon: Double) {
        val state = _uiState.value
        val skillId = state.activeSkillId ?: return
        
        viewModelScope.launch {
            val delta = ExperienceDelta(
                skillId = skillId,
                stepSequence = state.currentStepIndex,
                originalAction = com.bhoomibot.os.feature.autonomous.skills.models.ActionType.NAVIGATE,
                correctedLatitude = lat,
                correctedLongitude = lon,
                correctedParameters = mapOf("correction" to "true")
            )
            
            SkillLibrary.applyCorrection(getApplication(), delta)
            _uiState.value = _uiState.value.copy(isIntervened = false)
        }
    }
}
