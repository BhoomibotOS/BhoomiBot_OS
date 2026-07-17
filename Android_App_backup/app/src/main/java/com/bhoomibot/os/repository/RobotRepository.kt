package com.bhoomibot.os.repository

import com.bhoomibot.os.model.DriveCommand
import com.bhoomibot.os.model.RobotStatus

/** Boundary for a future ESP32/VCU transport implementation. */
interface RobotRepository {
    fun status(): RobotStatus
    fun sendDriveCommand(command: DriveCommand)
    fun updateSpeed(percent: Int)
    fun setPto(enabled: Boolean)
    fun setLights(enabled: Boolean)
}
