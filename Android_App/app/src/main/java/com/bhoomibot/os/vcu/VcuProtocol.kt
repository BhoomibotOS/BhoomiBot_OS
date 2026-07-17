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
 * IMPORTANT: the current VCU firmware (VCU_till_fixed_and_var_code_with_bluetooth.ino) speaks the
 * Dabble BLE gamepad protocol, NOT this raw serial protocol. These tokens are the agreed serial
 * contract to implement on the ESP32 side before enabling the real transport. Adjust the tokens
 * here (and the firmware parser) together when the two are brought in sync.
 */
fun DriveCommand.toProtocol(): String = when (this) {
    DriveCommand.FORWARD -> "F"
    DriveCommand.REVERSE -> "B"
    DriveCommand.LEFT -> "L"
    DriveCommand.RIGHT -> "R"
    DriveCommand.STOP -> "S"
    DriveCommand.EMERGENCY_STOP -> "E"
}

/** Speed set-point as a percentage (0–100). */
fun speedCommand(percent: Int): String = "SPD${percent.coerceIn(0, 100)}"

/** PTO (power-take-off) attachment on/off. */
fun ptoCommand(enabled: Boolean): String = if (enabled) "PTO1" else "PTO0"

/** Work lights on/off. */
fun lightsCommand(enabled: Boolean): String = if (enabled) "LGT1" else "LGT0"
