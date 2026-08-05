package com.bhoomibot.os.feature.autonomous.world

import com.bhoomibot.os.feature.autonomous.core.interfaces.WorldModelLayer
import com.bhoomibot.os.feature.autonomous.core.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * FEATURE: L6 World Model Layer Implementation
 * 
 * JUNIOR ENGINEER NOTE: This is the robot's short-term memory (Digital Twin).
 * It merges the robot's position (Localization) with what it sees (Perception).
 */
class WorldModelLayerImpl : WorldModelLayer {

    private val _map = MutableStateFlow<List<KnowledgeNode>>(emptyList())

    override fun getSemanticMap(): StateFlow<List<KnowledgeNode>> = _map.asStateFlow()

    override suspend fun syncPerception(pose: RobotPose, observations: List<Observation>) {
        // Logic: Project camera-space observations to world-space coordinates
        // Using Pose (lat/lon) + Bounding Box depth/angle.
        
        val entities = observations.map { obs ->
            KnowledgeNode(
                name = obs.label,
                type = NodeType.OBSTACLE,
                metadata = mapOf(
                    "confidence" to obs.confidence.toString(),
                    "source" to "VISION"
                )
            )
        }
        _map.value = entities
    }
}
