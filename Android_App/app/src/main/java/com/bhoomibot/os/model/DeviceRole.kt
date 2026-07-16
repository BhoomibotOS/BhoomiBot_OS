package com.bhoomibot.os.model

// The two physical roles a single BhoomiBot APK can serve.
// Chosen once at first launch and persisted locally in DevicePreferences.
enum class DeviceRole {
    // Handheld phone used by the person driving/monitoring the robot.
    OPERATOR,
    // Phone mounted on the robot itself; acts as the primary on-board computer.
    ROBOT
}

// Persisted string keys for DeviceRole (stable across versions).
fun DeviceRole.toKey(): String = name
fun String?.toDeviceRole(): DeviceRole? = if (this == DeviceRole.OPERATOR.name) DeviceRole.OPERATOR
    else if (this == DeviceRole.ROBOT.name) DeviceRole.ROBOT else null
