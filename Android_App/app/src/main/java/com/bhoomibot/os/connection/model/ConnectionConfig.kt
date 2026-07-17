// ============================================================================
// ConnectionConfig.kt
// ----------------------------------------------------------------------------
// The user-supplied settings that define a live-link session. For two phones to
// "meet" on the relay they must agree on serverUrl + robotId + sessionCode; the
// role tells the relay which side this phone plays. The video* fields only matter
// on the ROBOT side, which is the one encoding and publishing the camera feed.
// ============================================================================
package com.bhoomibot.os.connection.model

import com.bhoomibot.os.model.DeviceRole

/** Everything needed to open a live-link session on the relay server. */
data class ConnectionConfig(
    val serverUrl: String = "",
    val robotId: String = "",
    val sessionCode: String = "",
    val role: DeviceRole = DeviceRole.OPERATOR,
    val autoReconnect: Boolean = true,
    val videoFps: Int = 12,
    val videoQuality: VideoQuality = VideoQuality.MEDIUM
)

/** Target resolution/quality for the outgoing live video stream. */
enum class VideoQuality(
    val label: String,
    val longestSide: Int,
    val jpegQuality: Int
) {
    LOW("Low (360p)", 480, 45),
    MEDIUM("Medium (540p)", 720, 58),
    HIGH("High (720p)", 1280, 72);
}
