package com.bhoomibot.os.model

/** Operator-configurable increments used by manual controls. */
data class ControlCalibration(
    val driveStepMetersPerSecond: Int = 10,
    val maximumSpeedMetersPerSecond: Int = 10,
    val ptoStepPercent: Int = 10,
    val hydraulicHeightStepPercent: Int = 10
)
