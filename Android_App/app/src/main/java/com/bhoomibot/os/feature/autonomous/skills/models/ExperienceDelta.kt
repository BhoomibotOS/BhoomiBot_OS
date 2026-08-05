package com.bhoomibot.os.feature.autonomous.skills.models

import kotlinx.serialization.Serializable

/**
 * ExperienceDelta: Represents a human correction to a robotic action.
 * 
 * JUNIOR ENGINEER NOTE: This is how we "patch" the robot's brain. 
 * Instead of re-recording everything, we only record the fix.
 */
@Serializable
data class ExperienceDelta(
    val skillId: String,
    val stepSequence: Int,
    val originalAction: ActionType,
    val correctedLatitude: Double,
    val correctedLongitude: Double,
    val correctedParameters: Map<String, String>,
    val userReason: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
