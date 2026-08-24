package com.bhoomibot.os.feature.autonomous.planning

import com.bhoomibot.os.feature.autonomous.core.interfaces.PlannerLayer
import com.bhoomibot.sdk.*

/**
 * FEATURE: L2 Task Planner Implementation
 */
class PlannerLayerImpl : PlannerLayer {

    override suspend fun createPlan(intent: RobotIntent, context: List<com.bhoomibot.sdk.KnowledgeNode>): TaskPlan {
        val steps = mutableListOf<SkillStep>()

        when (intent.action) {
            RobotAction.TRANSPORT -> {
                repeat(intent.repeatCount) {
                    steps.add(SkillStep("NAVIGATE", mapOf("target" to (intent.source ?: "Shed"))))
                    steps.add(SkillStep("ATTACH", mapOf("tool" to (intent.subject ?: "LOAD"))))
                    steps.add(SkillStep("NAVIGATE", mapOf("target" to (intent.destination ?: "Home"))))
                    steps.add(SkillStep("DETACH"))
                }
            }
            RobotAction.NAVIGATE -> {
                steps.add(SkillStep("NAVIGATE", mapOf("target" to (intent.destination ?: "Home"))))
            }
            else -> {}
        }

        return TaskPlan(steps)
    }
}
