package com.bhoomibot.os.feature.autonomous.knowledge

import android.content.Context
import com.bhoomibot.os.feature.autonomous.core.interfaces.KnowledgeLayer
import com.bhoomibot.os.feature.autonomous.core.model.KnowledgeNode
import com.bhoomibot.os.feature.autonomous.core.model.NodeType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * FEATURE: L1 Knowledge Layer Implementation
 * 
 * JUNIOR ENGINEER NOTE: This is the robot's long-term memory. 
 * It stores semantic entities like "The Shed" or "Tomato Field" 
 * as nodes in a graph.
 */
class KnowledgeLayerImpl(context: Context) : KnowledgeLayer {

    // For now, keeping nodes in memory. 
    // TODO: Add persistence via DataStore or Room.
    private val _nodes = MutableStateFlow<List<KnowledgeNode>>(
        listOf(
            KnowledgeNode("Home", "Charging Base", NodeType.POINT, mapOf("lat" to "0.0", "lon" to "0.0"))
        )
    )

    override suspend fun queryNode(name: String): KnowledgeNode? {
        return _nodes.value.find { it.name.equals(name, ignoreCase = true) }
    }

    override suspend fun storeNode(node: KnowledgeNode) {
        val current = _nodes.value.toMutableList()
        current.removeAll { it.name == node.name } // Replace if exists
        current.add(node)
        _nodes.value = current
    }

    override fun getKnowledgeContext(): StateFlow<List<KnowledgeNode>> = _nodes.asStateFlow()
}
