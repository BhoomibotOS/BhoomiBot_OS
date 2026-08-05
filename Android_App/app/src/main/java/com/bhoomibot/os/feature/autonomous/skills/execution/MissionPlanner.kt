package com.bhoomibot.os.feature.autonomous.skills.execution

import com.bhoomibot.os.feature.autonomous.core.model.SkillExecution
import com.bhoomibot.os.feature.autonomous.core.model.TaskPlan
import com.bhoomibot.os.feature.autonomous.skills.models.DemonstratedSkill
import java.util.UUID

/**
 * MISSION PLANNER: The "Compiler" of the AgentOS.
 * 
 * It takes a high-level goal and a learned Skill, then "unrolls" it 
 * into a sequence of executable robotic steps.
 */
object MissionPlanner {

    /**
     * Creates an actionable TaskPlan from a demonstrated skill and user parameters.
     */
    fun createMissionFromSkill(skill: DemonstratedSkill, repeats: Int = 1): TaskPlan {
        val allSteps = mutableListOf<SkillExecution>()

        repeat(repeats) { iteration ->
            skill.steps.forEach { step ->
                allSteps.add(SkillExecution(
                    skillId = step.actionType.name,
                    parameters = step.parameters + mapOf(
                        "lat" to (step.latitude?.toString() ?: ""),
                        "lon" to (step.longitude?.toString() ?: ""),
                        "iteration" to iteration.toString()
                    )
                ))
            }
        }

        return TaskPlan(
            originalIntentId = "generated_${UUID.randomUUID()}",
            steps = allSteps
        )
    }
}
