package com.bhoomibot.os.navigation

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * AppRoute: Centralized navigation route definitions.
 */
object AppRoute {
    const val Onboarding = "onboarding"
    const val Dashboard = "dashboard"
    const val AgentChat = "agent"
    const val OperatorHome = "operator_home"
    const val RobotHome = "robot_home"
    const val Manual = "manual"
    const val Settings = "settings"
    const val SkillLibrary = "library"
    const val OperatorLive = "operator_live"
    const val Diagnostics = "diagnostics"
    const val Notifications = "notifications"
    const val RobotSettings = "robot_settings"
    const val Camera = "camera"
    const val ConnectionSettings = "connection_settings"
    const val LiveLinkSettings = "livelink_settings"

    fun playback(missionId: String) = "playback/$missionId"

    fun robotSection(title: String, description: String): String {
        val encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8.toString())
        val encodedDesc = URLEncoder.encode(description, StandardCharsets.UTF_8.toString())
        return "robot_section/$encodedTitle/$encodedDesc"
    }
}
