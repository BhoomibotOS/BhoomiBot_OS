package com.bhoomibot.os.feature.live

import com.bhoomibot.os.connection.model.PeerStatus
import com.bhoomibot.os.connection.model.RobotCommand
import com.bhoomibot.os.connection.model.VideoQuality
import com.bhoomibot.os.connection.transport.LiveConnectionState
import com.bhoomibot.os.model.DeviceRole

/**
 * Immutable snapshot of everything RobotLiveScreen needs to render.
 * RobotLiveViewModel holds one in a StateFlow and replaces it via copy().
 * Defaults describe the initial "screen just opened, not broadcasting" state.
 */
data class RobotLiveUiState(
    val connectionState: LiveConnectionState = LiveConnectionState.IDLE, // socket state -> ConnectionBadge
    val peerStatus: PeerStatus = PeerStatus(),                           // who is present -> PeerRow
    val isBroadcasting: Boolean = false,                                 // true while camera is streaming
    val framesSent: Int = 0,                                             // running count shown in the footer
    val lastCommand: RobotCommand? = null,                              // most recent command from the operator
    val error: String? = null,
    /** Loaded from this robot phone's live-link settings. */
    val isConfigurationReady: Boolean = false,                          // gates the START button until config loads
    // Effective (possibly network-downgraded) video settings; the CameraX
    // analyzer reads these each frame. See RobotLiveViewModel.updateNetworkProfile.
    val videoFps: Int = 12,
    val videoQuality: VideoQuality = VideoQuality.MEDIUM,
    val networkLabel: String = "Checking network…",                     // human-readable link type, e.g. "Wi-Fi"
    val isConstrainedNetwork: Boolean = false,                          // true = metered/cellular -> data saver on
    // Diagnostic: the role + meet-keys this phone is actually using, so a
    // role/key mismatch (the usual "connected but no video" cause) is visible
    // on screen instead of being invisible.
    val activeRole: DeviceRole? = null,
    val activeRobotId: String = "",
    val activeSession: String = ""
)
