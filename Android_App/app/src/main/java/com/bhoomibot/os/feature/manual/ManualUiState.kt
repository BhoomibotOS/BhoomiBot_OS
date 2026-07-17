package com.bhoomibot.os.feature.manual

import com.bhoomibot.os.model.DriveCommand
import com.bhoomibot.os.model.RobotStatus

// The two ways the operator can drive the robot from this screen.
enum class DrivingMode {
    DIGITAL,  // Tap buttons (FORWARD/REVERSE/LEFT/RIGHT/STOP)
    JOYSTICK  // Drag a virtual joystick
}

/**
 * Immutable snapshot of everything [ManualControlScreen] displays; produced by [ManualViewModel]
 * and consumed (read-only) by the Composables. It carries UI state only — the actual robot
 * transport (RobotRepository) stays in the ViewModel, outside the Compose layer. Each action
 * emits a fresh copy via `copy(...)`, so Compose re-renders just the parts that changed.
 */
data class ManualUiState(
    val robotStatus: RobotStatus = RobotStatus(),      // Battery/mode/GPS/camera/AI snapshot shown at top
    val drivingMode: DrivingMode = DrivingMode.DIGITAL, // Which drive control is active (DIGITAL or JOYSTICK)
    val vehicleSpeedPercent: Int = 0,                  // Current drive speed in m/s (0 = stopped)
    val ptoEnabled: Boolean = false,                   // PTO attachment on/off
    val ptoSpeedPercent: Int = 0,                      // PTO speed when enabled (0–100%)
    val lightsEnabled: Boolean = false,                // Work lights on/off
    val learningEnabled: Boolean = false,              // Learning/recording mode on/off (records manual driving for future AI training)
    val hydraulicEnabled: Boolean = false,             // Hydraulic lift on/off
    val hydraulicHeightPercent: Int = 0,              // Hydraulic height when enabled (0–100%); starts at 0 after restart
    val cameraLightEnabled: Boolean = false,           // Camera torch/flash on/off
    val isCameraMaximized: Boolean = false,            // true = camera shown full-screen (landscape)
    val cameraEnabled: Boolean = true,                 // Camera feed on/off (Live camera switch)
    val lastCommand: DriveCommand = DriveCommand.STOP, // Most recent drive command sent
    val bluetoothConnected: Boolean = false            // ESP32/Bluetooth link status (reserved)
)
