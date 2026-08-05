package com.bhoomibot.os.feature.autonomous.skills.learning

import com.bhoomibot.os.feature.autonomous.skills.models.*
import com.bhoomibot.os.model.MissionRecord
import com.bhoomibot.os.model.CommandRecord
import java.util.UUID

/**
 * EventAnalyzer: The "Brain" of the teaching system.
 * 
 * It scans a raw demonstration log and identifies logical segments
 * based on user markers and motion patterns.
 */
object EventAnalyzer {

    /**
     * Converts a raw recording into a logical skill workflow.
     */
    fun analyzeDemonstration(name: String, record: MissionRecord): DemonstratedSkill {
        val steps = mutableListOf<SkillStep>()
        var stepSequence = 0

        // 1. Identify "Anchors" (Points where user placed a Marker)
        val markedCommands = record.rawCommands.filter { it.marker != null }
        
        // 2. Add the Start Point as the first Navigation target
        if (record.rawCommands.isNotEmpty()) {
            val start = record.rawCommands.first()
            steps.add(SkillStep(
                sequence = stepSequence++,
                actionType = ActionType.NAVIGATE,
                latitude = start.latitude,
                longitude = start.longitude,
                parameters = mapOf("label" to "START")
            ))
        }

        // 3. Process each Marker into a logical step
        markedCommands.forEach { cmd ->
            val type = when (cmd.marker) {
                "PICKUP" -> ActionType.ATTACH
                "DROP" -> ActionType.DETACH
                "WAIT" -> ActionType.WAIT
                else -> ActionType.NAVIGATE
            }
            
            // Before the action, we must NAVIGATE to where it happened
            steps.add(SkillStep(
                sequence = stepSequence++,
                actionType = ActionType.NAVIGATE,
                latitude = cmd.latitude,
                longitude = cmd.longitude,
                stopOnArrival = true
            ))

            // Then perform the ACTION
            steps.add(SkillStep(
                sequence = stepSequence++,
                actionType = type,
                parameters = mapOf("timestamp" to cmd.timestamp.toString())
            ))
        }

        // 4. Add the End Point
        if (record.rawCommands.isNotEmpty()) {
            val end = record.rawCommands.last()
            steps.add(SkillStep(
                sequence = stepSequence++,
                actionType = ActionType.NAVIGATE,
                latitude = end.latitude,
                longitude = end.longitude,
                parameters = mapOf("label" to "END")
            ))
        }

        return DemonstratedSkill(
            id = UUID.randomUUID().toString(),
            name = name,
            steps = steps
        )
    }
}
