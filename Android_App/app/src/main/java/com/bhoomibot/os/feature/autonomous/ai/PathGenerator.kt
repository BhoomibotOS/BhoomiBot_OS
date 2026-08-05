package com.bhoomibot.os.feature.autonomous.ai

import com.bhoomibot.os.model.CommandRecord
import com.bhoomibot.os.model.MissionRecord
import com.bhoomibot.os.model.Waypoint
import java.util.UUID
import kotlin.math.*

/**
 * AI Path Generator that expands a template mission into a full field coverage pattern.
 * 
 * Reuses the logic of the recorded pass and duplicates it with spatial offsets.
 */
object PathGenerator {

    /**
     * Generates a full field mission from a single recorded pass.
     * 
     * @param template The recorded mission to use as a template.
     * @param implementWidthMeters The width of the mower/sprayer (the distance between passes).
     * @param numberOfPasses How many parallel lines to generate.
     */
    fun generateFieldCoverage(
        template: MissionRecord,
        implementWidthMeters: Double,
        numberOfPasses: Int
    ): MissionRecord {
        if (template.waypoints.size < 2) return template

        val firstWaypoint = template.waypoints.first()
        val lastWaypoint = template.waypoints.last()

        // 1. Calculate direction vector of the template pass
        val dLat = lastWaypoint.latitude - firstWaypoint.latitude
        val dLon = lastWaypoint.longitude - firstWaypoint.longitude

        // 2. Calculate the perpendicular (normal) vector for the offset
        // In 2D: (x, y) -> (-y, x)
        // We use a simplified approximation for small areas (meters -> degrees conversion)
        val latPerMeter = 1.0 / 111111.0
        val lonPerMeter = 1.0 / (111111.0 * cos(firstWaypoint.latitude * PI / 180.0))

        val normLat = -dLon * (latPerMeter / lonPerMeter)
        val normLon = dLat * (lonPerMeter / latPerMeter)
        
        val normLength = sqrt(normLat * normLat + normLon * normLon)
        val unitNormLat = normLat / normLength
        val unitNormLon = normLon / normLength

        val expandedWaypoints = mutableListOf<Waypoint>()
        val expandedCommands = mutableListOf<CommandRecord>()

        // 3. Generate parallel passes
        for (pass in 0 until numberOfPasses) {
            val offsetLat = unitNormLat * implementWidthMeters * latPerMeter * pass
            val offsetLon = unitNormLon * implementWidthMeters * lonPerMeter * pass

            val isReversed = pass % 2 != 0 // S-pattern: every second pass is driven in reverse direction
            
            val currentPassCommands = if (isReversed) {
                template.rawCommands.reversed().map { it.reversedDirection() }
            } else {
                template.rawCommands
            }

            val currentPassWaypoints = if (isReversed) {
                template.waypoints.reversed()
            } else {
                template.waypoints
            }

            // Add offset to every point in this pass
            currentPassWaypoints.forEach { wp ->
                expandedWaypoints.add(wp.copy(
                    latitude = wp.latitude + offsetLat,
                    longitude = wp.longitude + offsetLon,
                    timestamp = System.currentTimeMillis() // Update timestamps for execution flow
                ))
            }

            currentPassCommands.forEach { cmd ->
                expandedCommands.add(cmd.copy(
                    latitude = cmd.latitude + offsetLat,
                    longitude = cmd.longitude + offsetLon,
                    timestamp = System.currentTimeMillis()
                ))
            }
        }

        return template.copy(
            id = UUID.randomUUID().toString(),
            name = "${template.name} (Full Field AI)",
            waypoints = expandedWaypoints,
            rawCommands = expandedCommands,
            createdTimestamp = System.currentTimeMillis()
        )
    }

    /**
     * Helper to flip commands when reversing a pass in S-pattern.
     */
    private fun CommandRecord.reversedDirection(): CommandRecord {
        val reversedDrive = when (this.drive) {
            com.bhoomibot.os.model.DriveCommand.FORWARD -> com.bhoomibot.os.model.DriveCommand.REVERSE
            com.bhoomibot.os.model.DriveCommand.REVERSE -> com.bhoomibot.os.model.DriveCommand.FORWARD
            else -> this.drive
        }
        return this.copy(drive = reversedDrive)
    }
}
