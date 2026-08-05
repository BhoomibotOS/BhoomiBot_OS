package com.bhoomibot.os.feature.autonomous.simulation.hardware

import android.util.Log
import com.bhoomibot.os.feature.autonomous.simulation.engine.DigitalTwin
import com.bhoomibot.os.feature.autonomous.simulation.engine.RobotStateVector

/**
 * MOCK VCU: Emulates the ESP32 firmware logic.
 * 
 * JUNIOR ENGINEER NOTE: This is where we "fake" the hardware. It receives the 
 * exact same ASCII strings (F, B, SPD50) that the real robot would receive 
 * and updates the Digital Twin.
 */
class MockVcu {

    private var currentSpeedPercent = 0
    private var currentDirection = "S"

    /**
     * Receives a raw ASCII command and updates the virtual robot state.
     */
    fun onCommandReceived(cmd: String) {
        val trimmed = cmd.trim()
        Log.d("MockVCU", "Processing: $trimmed")

        when {
            trimmed.startsWith("SPD") -> {
                currentSpeedPercent = trimmed.substring(3).toIntOrNull() ?: 0
            }
            trimmed == "F" -> currentDirection = "F"
            trimmed == "B" -> currentDirection = "B"
            trimmed == "L" -> applyTurn(-5.0) // Virtual degrees
            trimmed == "R" -> applyTurn(5.0)
            trimmed == "S" || trimmed == "E" -> {
                currentSpeedPercent = 0
                currentDirection = "S"
            }
        }
        
        syncWithTwin()
    }

    private fun applyTurn(degrees: Double) {
        val current = DigitalTwin.state.value
        DigitalTwin.update(current.copy(yaw = current.yaw + degrees))
    }

    private fun syncWithTwin() {
        val current = DigitalTwin.state.value
        
        // Simple kinematic model for simulation:
        // Speed in m/s (assume 100% = 2.0 m/s)
        val speedMps = (currentSpeedPercent / 100f) * 2.0f
        
        DigitalTwin.update(current.copy(
            speed = speedMps,
            lastCommand = "${currentDirection}@${currentSpeedPercent}%"
        ))
    }
}
