package com.bhoomibot.os.feature.autonomous.skills

import com.bhoomibot.os.feature.autonomous.core.interfaces.SkillLayer
import com.bhoomibot.sdk.*

/**
 * FEATURE: L3 Skill Registry Implementation
 */
class SkillLayerImpl : SkillLayer {

    private val availableSkills = listOf(
        com.bhoomibot.sdk.Skill("NAVIGATE", "Drive to Location"),
        com.bhoomibot.sdk.Skill("ATTACH", "Pickup Object"),
        com.bhoomibot.sdk.Skill("DETACH", "Drop Object")
    )

    override suspend fun executeSkill(skillExecution: SkillStep): Trajectory {
        return when (skillExecution.skillId) {
            "NAVIGATE" -> {
                Trajectory(
                    waypoints = listOf(BrainWaypoint(0.0, 0.0)),
                    targetSpeedMps = 1.0f
                )
            }
            else -> Trajectory(emptyList(), 0f)
        }
    }

    override fun getAvailableSkills(): List<com.bhoomibot.sdk.Skill> = availableSkills
}
