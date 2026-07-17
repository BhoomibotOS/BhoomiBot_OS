/**
 * The two physical roles a single BhoomiBot APK can serve.
 *
 * Chosen once at first launch (Onboarding) and persisted in [com.bhoomibot.os.data.DevicePreferences].
 * The app routes to OperatorHome (handheld controller) or RobotHome (phone on the robot) based on
 * this value. The `toKey()`/`toDeviceRole()` helpers persist the enum as a stable string.
 */
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
