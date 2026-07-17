package com.bhoomibot.os.feature.connection

import com.bhoomibot.os.connection.model.VideoQuality
import com.bhoomibot.os.model.DeviceRole

/** Editable state for the live-link connection options screen. */
data class ConnectionOptionsUiState(
    val role: DeviceRole = DeviceRole.OPERATOR,
    val serverUrl: String = "",
    val robotId: String = "",
    val sessionCode: String = "",
    val autoReconnect: Boolean = true,
    val networkMode: PhoneNetworkMode = PhoneNetworkMode.INTERNET,
    val videoFps: Int = 12,
    val videoQuality: VideoQuality = VideoQuality.MEDIUM,
    val saved: Boolean = false
) {
    /**
     * Whether "Save & start" is allowed. Both phones must share a server URL, a Robot ID and a
     * session code to meet on the relay, and the fps must be in range.
     *
     * Scheme rule: on [PhoneNetworkMode.INTERNET] only a secure `wss://` URL is accepted (traffic
     * crosses the public internet); on [PhoneNetworkMode.LOCAL_WIFI] a plaintext `ws://` URL is
     * also allowed because both phones are on one trusted network.
     */
    val canStart: Boolean
        get() = (if (networkMode == PhoneNetworkMode.INTERNET) serverUrl.startsWith("wss://")
        else serverUrl.startsWith("wss://") || serverUrl.startsWith("ws://")) &&
            robotId.isNotBlank() &&
            sessionCode.isNotBlank() && videoFps in 1..30
}
