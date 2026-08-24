package com.bhoomibot.os.vcu

import com.bhoomibot.os.model.DriveCommand

// CONNECTION WORLD #1: VCU / ESP32 LOCAL link (classic Bluetooth or Wi-Fi hotspot on the field).
// This file has NO Android/socket dependencies — it is pure string-building: it turns app intents
// (drive commands, speed, PTO, lights) into the short ASCII tokens sent over the wire. It is the
// single source of truth for the serial "contract"; keep it in sync with the ESP32 firmware parser.

/**
 * Maps app intents to the VCU wire protocol. [ConnectionManager.send] appends "\n" after each
 * command, so every token here is a single line.
 *
 * IMPORTANT: these tokens are the agreed serial contract implemented by the ESP32 firmware
 * `VCU/VCU_joystick/VCU_joystick.ino`. The older `VCU_till_fixed_and_var_code_with_bluetooth.ino`
 * spoke the Dabble BLE gamepad protocol and is kept only as a reference. Adjust the tokens here
 * (and the firmware parser) together when the two are brought in sync.
 */
fun DriveCommand.toProtocol(): String = when (this) {
    DriveCommand.FORWARD -> "F"
    DriveCommand.REVERSE -> "B"
    DriveCommand.LEFT -> "L"
    DriveCommand.RIGHT -> "R"
    DriveCommand.STOP -> "S"
    DriveCommand.EMERGENCY_STOP -> "E"
}

/** Signed speed set-point as a percentage (-100..+100); sign = direction, magnitude = speed. */
fun speedCommand(percent: Int): String = "SPD${percent.coerceIn(-100, 100)}"

/** PTO (power-take-off) attachment on/off. */
fun ptoCommand(enabled: Boolean): String = if (enabled) "PTO1" else "PTO0"

/** Work lights on/off. */
fun lightsCommand(enabled: Boolean): String = if (enabled) "LGT1" else "LGT0"

/** Hydraulic lift height as PWM duty % (0 = retracted/off). */
fun hydraulicCommand(heightPercent: Int): String = "HYD${heightPercent.coerceIn(0, 100)}"

/** Horn — one-shot pulse (like GamePad.isSelectPressed()). */
fun hornCommand(): String = "HRN"

/** Trigger OTA maintenance mode. Robot will stop, lock brakes, and boot WiFi. */
fun otaMaintenanceCommand(): String = "OTA"
