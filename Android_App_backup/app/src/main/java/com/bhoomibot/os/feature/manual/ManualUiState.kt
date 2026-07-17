package com.bhoomibot.os.feature.manual

import com.bhoomibot.os.model.DriveCommand
import com.bhoomibot.os.model.RobotStatus

enum class DrivingMode { DIGITAL, JOYSTICK }

/** Immutable screen state; transport remains outside the Compose layer. */
data class ManualUiState(
    val robotStatus: RobotStatus = RobotStatus(),
    val drivingMode: DrivingMode = DrivingMode.DIGITAL,
    val vehicleSpeedPercent: Int = 0,
    val ptoEnabled: Boolean = false,
    val ptoSpeedPercent: Int = 0,
    val lightsEnabled: Boolean = false,
    val hydraulicEnabled: Boolean = false,
    val hydraulicHeightPercent: Int = 50,
    val cameraLightEnabled: Boolean = false,
    val isCameraMaximized: Boolean = false,
    val lastCommand: DriveCommand = DriveCommand.STOP,
    val bluetoothConnected: Boolean = false
)
