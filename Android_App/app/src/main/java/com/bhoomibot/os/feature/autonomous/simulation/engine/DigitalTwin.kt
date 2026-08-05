package com.bhoomibot.os.feature.autonomous.simulation.engine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * DIGITAL TWIN: The real-time mathematical mirror of the physical robot.
 */
data class RobotStateVector(
    val x: Double = 0.0,
    val y: Double = 0.0,
    val yaw: Double = 0.0, // Heading
    val speed: Float = 0f,
    val batteryPercent: Float = 100f,
    val motorRpm: Int = 0,
    val lastCommand: String = "IDLE",
    val timestamp: Long = System.currentTimeMillis()
)

object DigitalTwin {
    private val _state = MutableStateFlow(RobotStateVector())
    val state = _state.asStateFlow()

    /**
     * Updates the state vector. Used by the Physics Engine.
     */
    fun update(newState: RobotStateVector) {
        _state.value = newState
    }

    /**
     * Simulates battery drain based on motor activity.
     */
    fun consumePower(amount: Float) {
        val current = _state.value
        _state.value = current.copy(batteryPercent = (current.batteryPercent - amount).coerceAtLeast(0f))
    }
}
