package com.bhoomibot.os.feature.live

import androidx.compose.ui.graphics.ImageBitmap
import com.bhoomibot.os.connection.model.PeerStatus
import com.bhoomibot.os.connection.model.TelemetrySnapshot
import com.bhoomibot.os.connection.transport.LiveConnectionState
import com.bhoomibot.os.model.DeviceRole

/**
 * Immutable snapshot of everything OperatorLiveScreen needs to render.
 * OperatorLiveViewModel holds one of these in a StateFlow and replaces it (via
 * copy()) whenever any piece changes; the screen recomposes off the new value.
 * The defaults describe the initial "just opened, nothing received yet" state.
 */
data class OperatorLiveUiState(
    val connectionState: LiveConnectionState = LiveConnectionState.IDLE, // socket state -> ConnectionBadge
    val peerStatus: PeerStatus = PeerStatus(),                           // who is present -> PeerRow
    val telemetry: TelemetrySnapshot = TelemetrySnapshot(),             // latest robot read-out -> TelemetryOverlay
    val frame: ImageBitmap? = null,                                      // decoded live video frame; null = waiting
    val error: String? = null,                                          // set if something goes wrong (shown to user)
    // Operator's remote "live camera" switch. true = ask the robot to broadcast
    // (and show the feed here); false = ask the robot to stop. Keeps the link up.
    val liveCameraEnabled: Boolean = true,
    // Diagnostic: the role + meet-keys this phone is actually using, so a
    // role/key mismatch (the usual "connected but no video" cause) is visible
    // on screen instead of being invisible.
    val activeRole: DeviceRole? = null,
    val activeRobotId: String = "",
    val activeSession: String = ""
)
