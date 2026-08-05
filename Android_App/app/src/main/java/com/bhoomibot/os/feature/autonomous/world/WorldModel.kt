package com.bhoomibot.os.feature.autonomous.world

import com.bhoomibot.os.feature.autonomous.ai.DetectedObject
import com.bhoomibot.os.model.Waypoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * WorldModel: The digital twin of the field.
 */
object WorldModel {

    // Current spatial coordinates of the robot
    private val _robotPose = MutableStateFlow(Waypoint(0.0, 0.0, 0L, 0.0f))
    val robotPose: StateFlow<Waypoint> = _robotPose.asStateFlow()

    // Map of persistent obstacles (weeds, rocks)
    private val _semanticMap = MutableStateFlow<List<FieldEntity>>(emptyList())
    val semanticMap: StateFlow<List<FieldEntity>> = _semanticMap.asStateFlow()

    fun updatePerception(detections: List<DetectedObject>) {
        // Logic to project 2D camera boxes into 3D world coordinates
    }

    fun updateLocalization(location: Waypoint) {
        _robotPose.value = location
    }
}

/**
 * FieldEntity: Something in the real world.
 */
data class FieldEntity(
    val id: String,
    val type: String,
    val latitude: Double,
    val longitude: Double,
    val confidence: Float
)
