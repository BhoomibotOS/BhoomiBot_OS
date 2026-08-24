package com.bhoomibot.os.feature.autonomous.agent

import android.content.Context
import com.bhoomibot.ai.MasterBrain
import com.bhoomibot.ai.agent.BrainResponse
import com.bhoomibot.os.feature.autonomous.core.interfaces.AgentLayer
import com.bhoomibot.os.feature.autonomous.core.interfaces.AgentResponse
import com.bhoomibot.os.feature.autonomous.core.interfaces.KnowledgeLayer
import com.bhoomibot.os.feature.autonomous.core.interfaces.PlannerLayer
import com.bhoomibot.os.feature.autonomous.skills.library.SkillLibrary
import com.bhoomibot.os.feature.autonomous.skills.execution.MissionPlanner
import com.bhoomibot.os.feature.autonomous.skills.learning.TeachingManager
import java.util.Locale

/**
 * FEATURE: L0 Agent Layer Proxy
 */
class AgentLayerImpl(
    private val context: Context,
    private val knowledge: KnowledgeLayer,
    private val planner: PlannerLayer,
    private val masterBrain: MasterBrain
) : AgentLayer {

    override suspend fun processIntent(text: String): AgentResponse {
        val lower = text.lowercase(Locale.ROOT).trim()
        
        if (lower.contains("teach") || lower.contains("learn a new skill")) {
            val name = lower.substringAfter("skill", "").trim()
            if (name.isEmpty()) {
                return AgentResponse.Feedback("Sure! What should I call this new skill?")
            } else {
                TeachingManager.startTeaching(context, name)
                return AgentResponse.StartTeaching(name)
            }
        }

        // Use fully qualified name to avoid TaskPlan collision
        val response = masterBrain.processCommand(text)
        
        return when (response) {
            is BrainResponse.Plan -> {
                AgentResponse.PlanReady(com.bhoomibot.sdk.TaskPlan(response.steps))
            }
            is BrainResponse.Feedback -> AgentResponse.Feedback(response.message)
            is BrainResponse.Question -> AgentResponse.Feedback(response.query)
        }
    }
}
