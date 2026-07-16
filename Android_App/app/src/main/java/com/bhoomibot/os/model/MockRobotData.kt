package com.bhoomibot.os.model

// Realistic, hard-coded robot state used across Operator and Robot homes.
// This is a GUI-only placeholder: swap these objects for a real repository / ESP32 source later
// without touching any UI code.

// Status of the robot phone / onboard computer (Robot Mode home).
data class RobotSystemStatus(
    val systemUptime: String = "3h 42m",
    val cpuTempC: Int = 47,
    val batteryPercent: Int = 82,
    val cameraStatus: String = "Online • 1080p",
    val missionName: String = "Field A — Pass 2",
    val missionProgress: Int = 64,           // percent
    val motorHealth: String = "Nominal",
    val hydraulicHealth: String = "Nominal",
    val connectionType: String = "Local Wi-Fi",
    val signalStrength: Int = 4,             // 0-4 bars
    val currentMode: String = "Idle",
    val logsCount: Int = 128,
    val lastMaintenance: String = "2026-06-28"
)

// Centralized mock source. Replace with a repository implementation in a future phase.
object MockRobotData {
    val robotStatus: RobotStatus = RobotStatus(
        isOnline = true,
        batteryPercent = 82,
        mode = "Manual",
        mission = "Field A — Pass 2",
        gpsStatus = "Connected",
        cameraStatus = "Ready",
        aiStatus = "Standing by"
    )

    val systemStatus: RobotSystemStatus = RobotSystemStatus()
}
