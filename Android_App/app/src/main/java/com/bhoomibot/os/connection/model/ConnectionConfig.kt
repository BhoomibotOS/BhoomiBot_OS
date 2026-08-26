package com.bhoomibot.os.connection.model

import com.bhoomibot.os.model.DeviceRole

/** Everything needed to open a live-link session on the relay server. */
data class ConnectionConfig(
    val serverUrl: String = "wss://bhoomibot-os.madhumohan-contact.workers.dev/api/relay",
    val robotId: String = "BHOOMI-001",
    val sessionCode: String = "123",
    val role: DeviceRole = DeviceRole.OPERATOR,
    val autoReconnect: Boolean = true,
    val videoFps: Int = 12,
    val videoQuality: VideoQuality = VideoQuality.MEDIUM
)

/** 
 * Target resolution/quality for the outgoing live video stream. 
 * Optimized for Render's free tier bandwidth limits.
 */
enum class VideoQuality(
    val label: String,
    val longestSide: Int,
    val jpegQuality: Int
) {
    LOW("Standard", 640, 70),      // Good for low bandwidth
    MEDIUM("HD", 960, 80),         // Balanced for normal 4G
    HIGH("Full HD", 1280, 85),     // High quality for good 5G/Wi-Fi
    ULTRA("Ultra 1080p", 1920, 92); // Native 1080p for Hotspot
}
