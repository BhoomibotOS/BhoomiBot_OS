package com.bhoomibot.os.model

enum class DriveCommand { FORWARD, REVERSE, LEFT, RIGHT, STOP, EMERGENCY_STOP }

data class RobotStatus(
    val isOnline: Boolean = true,
    val batteryPercent: Int = 85,
    val mode: String = "Manual",
    val mission: String = "Idle",
    val gpsStatus: String = "Connected",
    val cameraStatus: String = "Ready",
    val aiStatus: String = "Standing by"
)
