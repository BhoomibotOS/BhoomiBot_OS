/**
 * Single decision point for which [RobotRepository] the app uses.
 *
 * `USE_REAL_TRANSPORT = false` (default) returns the no-op [LocalRobotRepository] so the app runs
 * with no robot paired. Set it `true` ONLY after the ESP32 firmware parses the raw serial protocol
 * in `vcu/VcuProtocol.kt` (today's firmware speaks Dabble BLE, not raw serial — see the in-file note).
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
 * NOTE: flip this to `true` ONLY after the VCU firmware parses the serial protocol defined in
 * `vcu/VcuProtocol.kt`. The current firmware (VCU_till_fixed_and_var_code_with_bluetooth.ino)
 * speaks the Dabble BLE gamepad protocol, so a raw-serial transport will not be understood yet.
 */
const val USE_REAL_TRANSPORT = false

fun provideRobotRepository(application: Application): RobotRepository =
    if (USE_REAL_TRANSPORT) VcuRobotRepository(application.applicationContext)
    else LocalRobotRepository()
