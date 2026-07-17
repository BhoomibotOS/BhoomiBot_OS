/**
 * In-memory, no-op implementation of [com.bhoomibot.os.repository.RobotRepository].
 *
 * This is the DEFAULT robot "transport" (selected by `USE_REAL_TRANSPORT = false`). Every command
 * is a safe no-op and [status] returns default values, so the app runs with NO robot paired. Swap
 * for [com.bhoomibot.os.repository.VcuRobotRepository] only after the ESP32 firmware can parse the
 * serial protocol in `vcu/VcuProtocol.kt`.
 */
package com.bhoomibot.os.data

import com.bhoomibot.os.model.DriveCommand
import com.bhoomibot.os.model.RobotStatus
import com.bhoomibot.os.repository.RobotRepository

/** In-memory transport placeholder.
 *  Right now every command is a no-op (does nothing) and status always returns default values.
 *  Replace these bodies with real ESP32 / Bluetooth communication when that hardware is ready. */
class LocalRobotRepository : RobotRepository {
    // Returns the default status object (robot appears online at 85% battery, Manual mode, etc.).
    override fun status() = RobotStatus()
    // No-op: would send a drive command to the robot over the real transport.
    override fun sendDriveCommand(command: DriveCommand) = Unit
    // No-op: would set the robot's speed.
    override fun updateSpeed(percent: Int) = Unit
    // No-op: would turn the PTO on/off.
    override fun setPto(enabled: Boolean) = Unit
    // No-op: would turn the work lights on/off.
    override fun setLights(enabled: Boolean) = Unit
}
