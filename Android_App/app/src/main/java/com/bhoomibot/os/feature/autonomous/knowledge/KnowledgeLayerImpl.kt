package com.bhoomibot.os.feature.autonomous.knowledge

import android.content.Context
import com.bhoomibot.os.feature.autonomous.core.interfaces.KnowledgeLayer
import com.bhoomibot.sdk.KnowledgeNode
import com.bhoomibot.sdk.NodeType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * FEATURE: L1 Knowledge Layer Implementation
 */
class KnowledgeLayerImpl(context: Context) : KnowledgeLayer {

    private val _nodes = MutableStateFlow<List<KnowledgeNode>>(
        listOf(
            KnowledgeNode(name = "Home", type = NodeType.POINT, metadata = mapOf("lat" to "0.0", "lon" to "0.0"))
        )
    )

    override suspend fun queryNode(name: String): KnowledgeNode? {
        return _nodes.value.find { it.name.equals(name, ignoreCase = true) }
    }

    override suspend fun storeNode(node: KnowledgeNode) {
        val current = _nodes.value.toMutableList()
        current.removeAll { it.name == node.name }
        current.add(node)
        _nodes.value = current
    }

    override fun getKnowledgeContext(): StateFlow<List<KnowledgeNode>> = _nodes.asStateFlow()
}
