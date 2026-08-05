package com.bhoomibot.os.feature.autonomous.simulation.engine

import kotlinx.coroutines.*
import kotlin.math.cos
import kotlin.math.sin

/**
 * SIM ENGINE: The "Master Clock" of the virtual world.
 * 
 * JUNIOR ENGINEER NOTE: This engine runs a physics loop 20 times per second.
 * It calculates where the robot should be based on its current speed and heading.
 */
class SimEngine(private val frequencyHz: Int = 20) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var loopJob: Job? = null

    /**
     * Starts the physics simulation loop.
     */
    fun start() {
        if (loopJob?.isActive == true) return
        
        loopJob = scope.launch {
            val dt = 1.0 / frequencyHz
            while (isActive) {
                stepPhysics(dt)
                delay((dt * 1000).toLong())
            }
        }
    }

    fun stop() {
        loopJob?.cancel()
    }

    /**
     * Calculates the next position based on kinematic motion.
     */
    private fun stepPhysics(dt: Double) {
        val current = DigitalTwin.state.value
        
        // Kinematics: X_next = X + v * cos(yaw) * dt
        // Heading is in degrees, convert to radians
        val yawRad = Math.toRadians(current.yaw)
        
        val dx = current.speed * cos(yawRad) * dt
        val dy = current.speed * sin(yawRad) * dt
        
        val newState = current.copy(
            x = current.x + dx,
            y = current.y + dy,
            timestamp = System.currentTimeMillis()
        )
        
        DigitalTwin.update(newState)
    }
}
