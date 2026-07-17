package com.bhoomibot.os.data

import com.bhoomibot.os.model.DriveCommand
import com.bhoomibot.os.model.RobotStatus
import com.bhoomibot.os.repository.RobotRepository

/** In-memory transport placeholder. Replace internals with ESP32 communication when available. */
class LocalRobotRepository : RobotRepository {
    override fun status() = RobotStatus()
    override fun sendDriveCommand(command: DriveCommand) = Unit
    override fun updateSpeed(percent: Int) = Unit
    override fun setPto(enabled: Boolean) = Unit
    override fun setLights(enabled: Boolean) = Unit
}
