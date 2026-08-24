package com.bhoomibot.ai.world

import com.bhoomibot.sdk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * L6 World Model: The Digital Twin.
 * Merges localization and perception data into a persistent state.
 */
class DigitalTwin {

    private val _currentPose = MutableStateFlow(RobotPose(0.0, 0.0, 0.0, 0.0, 0f, 0L))
    val currentPose = _currentPose.asStateFlow()

    private val _entities = MutableStateFlow<List<Observation>>(emptyList())
    val entities = _entities.asStateFlow()

    fun updatePose(pose: RobotPose) {
        _currentPose.value = pose
    }

    fun updateObservations(observations: List<Observation>) {
        // Logic: Filter and map short-term memory here
        _entities.value = observations
    }

    /** Returns the center-line steering offset based on detected row crops */
    fun calculatePathOffset(): Float {
        // AI-Fix: Add math for visual navigation here
        return 0f
    }
}
