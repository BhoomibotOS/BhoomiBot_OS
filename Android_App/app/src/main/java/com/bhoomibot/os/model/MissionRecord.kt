/**
 * Complete recorded mission with waypoints and command sequence.
 *
 * Represents a full recording session that can be saved, loaded, and replayed.
 * Stored as JSON in app's internal files directory under /autonomy/missions/.
 */
package com.bhoomibot.os.model

import kotlinx.serialization.Serializable

/** Complete recorded mission with waypoints and command sequence */
@Serializable
data class MissionRecord(
    val id: String,
    val name: String,
    val waypoints: List<Waypoint>,
    val rawCommands: List<CommandRecord>,
    val operatorId: String,
    val createdTimestamp: Long
)