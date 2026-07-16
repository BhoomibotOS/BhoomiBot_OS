package com.bhoomibot.os.model

/** Operator-configurable increments used by manual controls.
 *  These values are edited in the Settings screen and change how big each manual adjustment is. */
data class ControlCalibration(
    // How much speed (in m/s) is added per single tap of a digital drive button (FORWARD/REVERSE/LEFT/RIGHT).
    val driveStepMetersPerSecond: Int = 10,
    // Hard speed ceiling for the robot, in m/s. Digital drive can never exceed this.
    val maximumSpeedMetersPerSecond: Int = 10,
    // How much the PTO speed slider jumps per step (in %), set in Settings.
    val ptoStepPercent: Int = 10,
    // How much the hydraulic height slider jumps per step (in %), set in Settings.
    val hydraulicHeightStepPercent: Int = 10
)
