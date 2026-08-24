/**
 * The boundary every screen uses to talk to the robot.
 *
 * Hiding the transport behind this interface means the UI never knows whether commands go to a
 * fake (LocalRobotRepository) or a real ESP32 (VcuRobotRepository) — you can swap them without
 * touching any UI. This is the LOCAL VCU / Bluetooth world (making the robot move), distinct from
 * the internet relay link in `connection/`.
 */
package com.bhoomibot.os.repository

import com.bhoomibot.os.model.DriveCommand
import com.bhoomibot.os.model.RobotStatus
import kotlinx.coroutines.flow.StateFlow

/** Boundary for a future ESP32/VCU transport implementation.
 *  Every screen talks to the robot ONLY through this interface, so we can later swap the
 *  fake/in-memory version (LocalRobotRepository) for a real Bluetooth/serial connection
 *  without touching any UI code. */
interface RobotRepository {
    // Returns the latest known robot status snapshot (battery, mode, GPS, etc.).
    fun status(): RobotStatus

    // Returns the current connection state of the hardware transport.
    val isConnected: StateFlow<Boolean>

    // Real-time RPM feedback from the motors (Left, Right)
    val rpmData: StateFlow<Pair<Int, Int>>

    // Sends a movement/stop command to the robot (FORWARD, STOP, EMERGENCY_STOP, ...).
    fun sendDriveCommand(command: DriveCommand)

    // Sets the target drive speed (value is in m/s).
    fun updateSpeed(percent: Int)

    // Turns the PTO (power-take-off) attachment on/off.
    fun setPto(enabled: Boolean)

    // Turns the work lights on/off.
    fun setLights(enabled: Boolean)

    // Sets the hydraulic lift height as a PWM duty % (0 = retracted/off).
    fun setHydraulic(heightPercent: Int)

    // Pulses the horn once (one-shot, like GamePad.isSelectPressed()).
    fun horn()

    // Triggers the OTA maintenance mode on the VCU.
    fun triggerOta()

    // Releases the underlying transport (Bluetooth socket / WiFi connection). No-op by default.
    fun disconnect() = Unit
}
