/**
 * Operator-configurable increments used by the Manual controls.
 *
 * Edited in Settings and held in the in-memory [com.bhoomibot.os.data.ControlCalibrationStore]
 * (Settings writes, Manual reads). `driveStepMetersPerSecond` is how much each digital drive tap
 * adds; `maximumSpeedMetersPerSecond` is the hard ceiling; the `*StepPercent` values are the jump
 * size for the PTO / hydraulic sliders.
 */
package com.bhoomibot.os.model

/** Operator-configurable increments used by manual controls.
 *  These values are edited in the Settings screen and change how big each manual adjustment is. */
data class ControlCalibration(
    // How much speed (in m/s) is added per single tap of a digital drive button (FORWARD/REVERSE/LEFT/RIGHT).
    // Kept well below maximumSpeedMetersPerSecond so a tap ramps speed gradually (e.g. ~5 taps to full).
    val driveStepMetersPerSecond: Int = 1,
    // Hard speed ceiling for the robot, in m/s. Digital drive can never exceed this.
    val maximumSpeedMetersPerSecond: Int = 50,
    // How much the PTO speed slider jumps per step (in %), set in Settings.
    val ptoStepPercent: Int = 0,
    // How much the hydraulic height slider jumps per step (in %), set in Settings.
    val hydraulicHeightStepPercent: Int = 0
)
