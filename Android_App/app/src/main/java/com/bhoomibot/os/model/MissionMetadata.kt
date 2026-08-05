package com.bhoomibot.os.model

import kotlinx.serialization.Serializable

/**
 * Lightweight metadata describing a stored mission.
 *
 * Used by mission libraries and UI listings to display mission summaries
 * without loading the full command sequence.
 *
 * @property id Unique mission identifier (UUID)
 * @property name Human-readable mission name
 * @property durationSeconds Approximate mission duration in seconds
 * @property waypointCount Number of recorded GPS waypoints
 * @property commandCount Number of recorded control commands
 */
@Serializable
data class MissionMetadata(
    val id: String,
    val name: String,
    val durationSeconds: Int,
    val waypointCount: Int,
    val commandCount: Int
)