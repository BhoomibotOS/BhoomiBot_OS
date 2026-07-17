package com.bhoomibot.os.connection.model

import com.bhoomibot.os.model.DriveCommand

/** Drive/auxiliary command sent OPERATOR -> ROBOT. */
data class RobotCommand(
    val drive: DriveCommand = DriveCommand.STOP, // direction/gear intent
    val speedPercent: Int = 0,                   // 0..100 throttle
    val emergencyStop: Boolean = false,          // hard stop overrides drive
    // pto (power take-off) and lights are tri-state on purpose: null means
    // "leave the current setting unchanged", true/false explicitly toggle it.
    val pto: Boolean? = null,
    val lights: Boolean? = null
)
