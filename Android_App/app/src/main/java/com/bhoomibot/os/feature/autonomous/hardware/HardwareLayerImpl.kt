package com.bhoomibot.os.feature.autonomous.hardware

import com.bhoomibot.os.feature.autonomous.core.interfaces.HardwareLayer
import com.bhoomibot.os.feature.autonomous.core.model.VelocityCommand
import com.bhoomibot.os.model.DriveCommand
import com.bhoomibot.os.repository.RobotRepository

/**
 * FEATURE: L8 Hardware Layer Adapter
 * 
 * JUNIOR ENGINEER NOTE: This class is a "Bridge". It takes the standardized
 * VelocityCommand and translates it into the specific ASCII protocol our VCU understands.
 */
class HardwareLayerImpl(private val repository: RobotRepository) : HardwareLayer {

    override suspend fun sendMotion(command: VelocityCommand) {
        // Translate Normalized Velocity to VCU Percentages
        val speedPercent = (command.linearVelocity * 100).toInt()
        val steer = when {
            command.angularVelocity < -0.2f -> DriveCommand.LEFT
            command.angularVelocity > 0.2f -> DriveCommand.RIGHT
            command.linearVelocity > 0.1f -> DriveCommand.FORWARD
            command.linearVelocity < -0.1f -> DriveCommand.REVERSE
            else -> DriveCommand.STOP
        }

        repository.updateSpeed(speedPercent)
        repository.sendDriveCommand(steer)
    }

    override suspend fun triggerAuxiliary(toolId: String, action: String) {
        when (toolId) {
            "PTO" -> repository.setPto(action == "ON")
            "LIGHTS" -> repository.setLights(action == "ON")
            "HORN" -> repository.horn()
        }
    }
}
