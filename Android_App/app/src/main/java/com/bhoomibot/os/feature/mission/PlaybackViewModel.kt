package com.bhoomibot.os.feature.mission

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bhoomibot.os.feature.autonomous.AutonomyManager
import com.bhoomibot.os.feature.autonomous.WaypointTracker
import com.bhoomibot.os.model.Waypoint
import com.bhoomibot.sdk.RobotPose
import com.bhoomibot.os.repository.RobotRepository
import com.bhoomibot.os.repository.provideRobotRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * PlaybackViewModel: Manages the UI state for Mission Replay.
 * AI-Fix: Redirected to Standalone SDK RobotPose.
 */
class PlaybackViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: RobotRepository = provideRobotRepository(application)
    private val perceptionEngine = AutonomyManager.getPerceptionEngine(application)
    private val planExecutor = AutonomyManager.getPlanExecutor(application)

    private val _uiState = MutableStateFlow(PlaybackUiState())
    val uiState = _uiState.asStateFlow()

    private var waypointTracker: WaypointTracker? = null

    init {
        viewModelScope.launch {
            perceptionEngine.steeringOffset.collect { offset ->
                _uiState.update { it.copy(steeringAdjustment = offset) }
            }
        }

        viewModelScope.launch {
            repository.isConnected.collectLatest { connected ->
                _uiState.update { it.copy(isHardwareOnline = connected) }
            }
        }

        viewModelScope.launch {
            planExecutor.uiState.collect { execState ->
                _uiState.update { it.copy(
                    isActive = execState.isActive,
                    missionName = if (execState.isActive) "Executing: ${execState.currentStepLabel}" else it.missionName
                ) }
            }
        }

        AutonomyManager.pendingPlan?.let { plan ->
            loadPlan(plan)
            AutonomyManager.pendingPlan = null // Consume it
        }
    }

    fun startMission(waypoints: List<Waypoint>) {
        waypointTracker = WaypointTracker(waypoints)
        _uiState.update { it.copy(isActive = true, remainingWaypoints = waypoints.size) }
    }

    fun loadPlan(plan: com.bhoomibot.sdk.TaskPlan) {
        _uiState.update { it.copy(
            isActive = true,
            remainingWaypoints = plan.steps.size,
            missionName = "Autonomous Plan",
            steps = plan.steps
        ) }
        planExecutor.execute(plan)
    }

    fun stopMission() {
        planExecutor.stop()
        _uiState.update { it.copy(isActive = false) }
    }
}

data class PlaybackUiState(
    val isActive: Boolean = false,
    val currentPose: RobotPose? = null,
    val steeringAdjustment: Float = 0f,
    val isHardwareOnline: Boolean = false,
    val remainingWaypoints: Int = 0,
    val missionName: String = "",
    val steps: List<com.bhoomibot.sdk.SkillStep> = emptyList()
)
