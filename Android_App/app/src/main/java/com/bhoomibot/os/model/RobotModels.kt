package com.bhoomibot.os.model

// Every movement/stop instruction that can be sent to the robot's drive system.
// Used by Manual mode buttons and the joystick to tell the robot what to do.
enum class DriveCommand {
    FORWARD,          // Move forward
    REVERSE,         // Move backward
    LEFT,            // Turn left
    RIGHT,           // Turn right
    STOP,            // Normal stop (cancel current movement)
    EMERGENCY_STOP   // Immediate hard stop (safety)
}

// A snapshot of the robot's current health/state shown on the dashboard and manual screens.
// All fields have safe default values so the UI can render before real data arrives.
data class RobotStatus(
    val isOnline: Boolean = true,        // true = robot is connected/reachable, false = OFFLINE
    val batteryPercent: Int = 85,        // Remaining battery level, 0–100%
    val mode: String = "Manual",         // Current control mode (e.g. "Manual", future "Autonomous")
    val mission: String = "Idle",        // What the robot is currently doing (e.g. "Idle", a task name)
    val gpsStatus: String = "Connected", // GPS receiver state (e.g. "Connected", "No Signal")
    val cameraStatus: String = "Ready",  // Camera state (e.g. "Ready", "Offline")
    val aiStatus: String = "Standing by" // AI/decision system state (e.g. "Standing by", "Active")
)
