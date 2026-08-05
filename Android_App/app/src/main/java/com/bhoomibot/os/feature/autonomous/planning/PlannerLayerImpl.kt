package com.bhoomibot.os.feature.autonomous.planning

import com.bhoomibot.os.feature.autonomous.core.interfaces.PlannerLayer
import com.bhoomibot.os.feature.autonomous.core.model.*

/**
 * FEATURE: L2 Task Planner Implementation
 */
class PlannerLayerImpl : PlannerLayer {

    override suspend fun createPlan(intent: Intent, context: List<KnowledgeNode>): TaskPlan {
        val steps = mutableListOf<SkillExecution>()

        when (intent.action) {
            "TRANSPORT" -> {
                val subject = intent.subject ?: "LOAD"
                val source = intent.source ?: "Shed"
                val dest = intent.destination ?: "Home"
                
                // For 'N' trips, repeat the sequence
                repeat(intent.repeatCount) {
                    steps.add(SkillExecution("NAVIGATE", mapOf("target" to source)))
                    steps.add(SkillExecution("ATTACH", mapOf("tool" to subject)))
                    steps.add(SkillExecution("NAVIGATE", mapOf("target" to dest)))
                    steps.add(SkillExecution("DETACH", emptyMap()))
                }
            }
            "LEARN" -> {
                val name = intent.subject ?: "Unknown"
                steps.add(SkillExecution("DRIVE_TO_LEARN", mapOf("name" to name)))
            }
            "NAVIGATE" -> {
                val dest = intent.destination ?: "Home"
                steps.add(SkillExecution("NAVIGATE", mapOf("target" to dest)))
            }
        }

        return TaskPlan(
            originalIntentId = intent.id,
            steps = steps
        )
    }
}
