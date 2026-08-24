package com.bhoomibot.os.feature.autonomous.world

import com.bhoomibot.os.feature.autonomous.core.interfaces.WorldModelLayer
import com.bhoomibot.sdk.RobotPose
import com.bhoomibot.sdk.Observation
import com.bhoomibot.sdk.KnowledgeNode
import com.bhoomibot.sdk.NodeType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * FEATURE: L6 World Model Layer Implementation
 */
class WorldModelLayerImpl : WorldModelLayer {

    private val _map = MutableStateFlow<List<KnowledgeNode>>(emptyList())

    override fun getSemanticMap(): StateFlow<List<KnowledgeNode>> = _map.asStateFlow()

    override suspend fun syncPerception(pose: RobotPose, observations: List<Observation>) {
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
