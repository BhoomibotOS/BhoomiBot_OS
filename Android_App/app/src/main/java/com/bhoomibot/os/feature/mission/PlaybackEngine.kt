package com.bhoomibot.os.feature.mission

import com.bhoomibot.os.data.LocalRobotRepository
import com.bhoomibot.os.data.LocationTracker
import com.bhoomibot.os.feature.autonomous.AutonomyState
import com.bhoomibot.os.feature.autonomous.AutonomyStateMachine
import com.bhoomibot.os.feature.autonomous.WaypointTracker
import com.bhoomibot.os.feature.autonomous.ai.PerceptionEngine
import com.bhoomibot.os.model.CommandRecord
import com.bhoomibot.os.model.DriveCommand
import com.bhoomibot.os.model.MissionRecord
import com.bhoomibot.os.repository.RobotRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Engine that replays recorded missions through the existing robot command pipeline.
 *
 * Reuses RobotRepository interface to send DriveCommand objects and preserve recorded timing.
 * Does not create new command pipelines.
 */
class PlaybackEngine(
    private val repo: RobotRepository = LocalRobotRepository(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + Job()),
    private val locationTracker: LocationTracker? = null,
    private val stateMachine: AutonomyStateMachine? = null,
    private val perceptionEngine: PerceptionEngine? = null
) {
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private var _currentPosition = 0
    val currentPosition: Int = _currentPosition

    private var _totalCommands = 0
    val totalCommands: Int = _totalCommands

    private var _playbackSpeed = 1.0f
    val playbackSpeed: Float = _playbackSpeed

    private var _progressPercent = MutableStateFlow(0)
    val progressPercent = _progressPercent.asStateFlow()

    private var playbackJob: Job? = null
    private var waypointTracker: WaypointTracker? = null

    /** Listener for playback events */
    interface PlaybackListener {
        fun onPlaybackStarted()
        fun onCommandExecuted(command: CommandRecord, position: Int)
        fun onPlaybackCompleted()
        fun onPlaybackError(error: String)
        fun onPlaybackPaused()
        fun onPlaybackResumed()
    }

    /** Start replaying a mission */
    fun startPlayback(mission: MissionRecord, listener: PlaybackListener) {
        if (stateMachine != null) {
            if (!stateMachine.transitionTo(AutonomyState.EXECUTING)) return
        } else {
            if (_isPlaying.value || _isPaused.value) return
        }
        
        // Validation
        if (mission.rawCommands.isEmpty()) {
            stateMachine?.transitionTo(AutonomyState.ERROR)
            listener.onPlaybackError("Empty mission")
            return
        }

        _totalCommands = mission.rawCommands.size
        _currentPosition = 0
        _isPlaying.value = true
        _isPaused.value = false
        _progressPercent.value = 0
        
        waypointTracker = if (mission.waypoints.isNotEmpty()) {
            WaypointTracker(mission.waypoints)
        } else null

        locationTracker?.startTracking()
        listener.onPlaybackStarted()

        playbackJob = scope.launch {
            while (_currentPosition < mission.rawCommands.size && _isPlaying.value) {
                if (!_isPaused.value) {
                    val command = mission.rawCommands[_currentPosition]

                    // Calculate adjustments if waypoints are available
                    var adjustedSpeed = command.speedPercent
                    var adjustedDrive = command.drive
                    val loc = locationTracker?.currentLocation?.value
                    
                    // 1. GPS Slowdown
                    if (loc != null && waypointTracker != null) {
                        val multiplier = waypointTracker!!.getSpeedMultiplier(loc.latitude, loc.longitude)
                        adjustedSpeed = (adjustedSpeed * multiplier).toInt()
                        waypointTracker!!.updateProgress(loc.latitude, loc.longitude)
                        _progressPercent.value = waypointTracker!!.getProgressPercent()
                    }

                    // 2. Vision Steering Adjustment (AI-004)
                    perceptionEngine?.steeringOffset?.value?.let { offset ->
                        if (offset > 0.3f) adjustedDrive = DriveCommand.RIGHT
                        if (offset < -0.3f) adjustedDrive = DriveCommand.LEFT
                    }

                    // Send command through the existing RobotRepository pipeline
                    repo.sendDriveCommand(adjustedDrive)
                    repo.updateSpeed(adjustedSpeed.coerceIn(-100, 100))
                    repo.setPto(command.ptoEnabled)
                    repo.setLights(command.lightsEnabled)
                    repo.setHydraulic(command.hydraulicHeightPercent)
                    if (command.hornTriggered) {
                        repo.horn()
                    }

                    _currentPosition++
                    listener.onCommandExecuted(command, _currentPosition - 1)
                    if (waypointTracker == null) {
                        _progressPercent.value = (_currentPosition * 100 / _totalCommands)
                    }

                    // Calculate delay to next command
                    if (_currentPosition < mission.rawCommands.size) {
                        val nextCommand = mission.rawCommands[_currentPosition]
                        val delay = ((nextCommand.timestamp - command.timestamp) / _playbackSpeed).toLong()
                        delay(delay.coerceAtLeast(0L))
                    }
                } else {
                    delay(100) // Poll while paused
                }
            }
            if (_isPlaying.value) {
                _isPlaying.value = false
                locationTracker?.stopTracking()
                stateMachine?.transitionTo(AutonomyState.COMPLETED)
                listener.onPlaybackCompleted()
            }
        }
    }

    /** Stop current playback */
    fun stopPlayback() {
        _isPlaying.value = false
        _isPaused.value = false
        playbackJob?.cancel()
        stateMachine?.transitionTo(AutonomyState.IDLE)
    }

    /** Pause current playback */
    fun pausePlayback() {
        if (stateMachine != null) {
            if (!stateMachine.transitionTo(AutonomyState.PAUSED)) return
        } else {
            if (!_isPlaying.value || _isPaused.value) return
        }
        _isPaused.value = true
    }

    /** Resume paused playback */
    fun resumePlayback() {
        if (stateMachine != null) {
            if (!stateMachine.transitionTo(AutonomyState.EXECUTING)) return
        } else {
            if (!_isPlaying.value || !_isPaused.value) return
        }
        _isPaused.value = false
    }

    /** Set playback speed (1.0 = normal, 2.0 = 2x speed, etc.) */
    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed = speed
    }

    /** Reset engine to initial state */
    fun reset() {
        stopPlayback()
        _currentPosition = 0
        _totalCommands = 0
        _playbackSpeed = 1.0f
    }
}
