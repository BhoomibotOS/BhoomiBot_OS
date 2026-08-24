package com.bhoomibot.os.feature.autonomous.skills.execution

import com.bhoomibot.sdk.SkillStep
import com.bhoomibot.sdk.TaskPlan
import com.bhoomibot.os.feature.autonomous.skills.models.DemonstratedSkill
import java.util.UUID

/**
 * MISSION PLANNER: The "Compiler" of the AgentOS.
 */
object MissionPlanner {

    /**
     * Creates an actionable TaskPlan from a demonstrated skill and user parameters.
     */
    fun createMissionFromSkill(skill: DemonstratedSkill, repeats: Int = 1): TaskPlan {
        val allSteps = mutableListOf<SkillStep>()

        repeat(repeats) { iteration ->
            skill.steps.forEach { step ->
                allSteps.add(SkillStep(
                    skillId = step.actionType.name,
                    parameters = step.parameters + mapOf(
                        "lat" to (step.latitude?.toString() ?: ""),
                        "lon" to (step.longitude?.toString() ?: ""),
                        "iteration" to iteration.toString()
                    )
                ))
            }
        }

        return TaskPlan(allSteps)
    }
}
