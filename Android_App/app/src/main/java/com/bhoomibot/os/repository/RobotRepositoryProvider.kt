/**
 * Single decision point for which [RobotRepository] the app uses.
 *
 * `USE_REAL_TRANSPORT = true` drives the real ESP32/VCU over [ConnectionManager]. The ESP32 firmware
 * that parses the raw serial protocol in `vcu/VcuProtocol.kt` is `VCU/VCU_joystick/VCU_joystick.ino`.
 * Set it `false` to fall back to the no-op [LocalRobotRepository] (app runs with no robot paired).
 */
package com.bhoomibot.os.repository

import android.app.Application
import com.bhoomibot.os.data.LocalRobotRepository

/**
 * Single decision point for which [RobotRepository] the app uses.
 *
 * - `false` (default): the in-memory fake [LocalRobotRepository] — the app runs with no robot
 *   paired and every command is a safe no-op.
 * - `true`: the real [VcuRobotRepository], which drives the ESP32 over [ConnectionManager].
 *
 * NOTE: the matching ESP32 firmware is `VCU/VCU_joystick/VCU_joystick.ino`, which parses the
 * serial protocol defined here. The older `VCU_till_fixed_and_var_code_with_bluetooth.ino` spoke
 * the Dabble BLE gamepad protocol and is kept only as a reference demo.
 */
const val USE_REAL_TRANSPORT = true

fun provideRobotRepository(application: Application): RobotRepository =
    if (USE_REAL_TRANSPORT) VcuRobotRepository(application.applicationContext)
    else LocalRobotRepository()
