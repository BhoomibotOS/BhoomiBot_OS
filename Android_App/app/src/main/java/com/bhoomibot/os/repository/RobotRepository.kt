package com.bhoomibot.os.repository

import com.bhoomibot.os.model.DriveCommand
import com.bhoomibot.os.model.RobotStatus

/** Boundary for a future ESP32/VCU transport implementation.
 *  Every screen talks to the robot ONLY through this interface, so we can later swap the
 *  fake/in-memory version (LocalRobotRepository) for a real Bluetooth/serial connection
 *  without touching any UI code. */
interface RobotRepository {
    // Returns the latest known robot status snapshot (battery, mode, GPS, etc.).
    fun status(): RobotStatus

    // Sends a movement/stop command to the robot (FORWARD, STOP, EMERGENCY_STOP, ...).
    fun sendDriveCommand(command: DriveCommand)

    // Sets the target drive speed (value is in m/s).
    fun updateSpeed(percent: Int)

    // Turns the PTO (power-take-off) attachment on/off.
    fun setPto(enabled: Boolean)

    // Turns the work lights on/off.
    fun setLights(enabled: Boolean)

    // Releases the underlying transport (Bluetooth socket / WiFi connection). No-op by default.
    fun disconnect() = Unit
}
