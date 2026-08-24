package com.bhoomibot.ai.planner

import com.bhoomibot.sdk.*

/**
 * L2 Task Planner: Strategic Logic
 */
class TaskPlanner {

    fun generatePlan(intent: RobotIntent): TaskPlan {
        val steps = mutableListOf<SkillStep>()

        when (intent.action) {
            RobotAction.TRANSPORT -> {
                repeat(intent.repeatCount) {
                    steps.add(SkillStep("NAVIGATE", mapOf("target" to (intent.source ?: "Shed"))))
                    steps.add(SkillStep("ATTACH", mapOf("subject" to (intent.subject ?: "LOAD"))))
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
