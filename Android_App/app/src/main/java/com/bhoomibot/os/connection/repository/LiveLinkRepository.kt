package com.bhoomibot.os.connection.repository

import com.bhoomibot.os.connection.model.ConnectionConfig
import com.bhoomibot.os.connection.model.LiveFrame
import com.bhoomibot.os.connection.model.PeerStatus
import com.bhoomibot.os.connection.model.RobotCommand
import com.bhoomibot.os.connection.model.TelemetrySnapshot
import com.bhoomibot.os.connection.transport.LiveConnectionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Role-agnostic live-link boundary. Every screen talks to the robot/operator
 * only through this interface, exactly like [com.bhoomibot.os.repository.RobotRepository]
 * hides the VCU transport. The relay WebSocket lives behind it.
 */
// Note on flow types below: StateFlow is used for things that have a "current
// value" you always want the latest of (connection state, telemetry, peer status);
// plain Flow is used for discrete event streams where each item matters once
// (video frames, incoming commands). Producer-only members are split by role —
// the OPERATOR consumes frames/telemetry and sends commands; the ROBOT publishes
// frames/telemetry and consumes commands.
interface LiveLinkRepository {
    /** Socket lifecycle (IDLE / CONNECTING / CONNECTED / RECONNECTING / ERROR). */
    val connectionState: StateFlow<LiveConnectionState>

    /** Last human-readable connection failure reason (null when healthy). */
    val connectionError: StateFlow<String?>

    /** Who is present in the session (relay's PEER_STATUS broadcasts). */
    val peerStatus: StateFlow<PeerStatus>

    /** Inbound video frames (operator consumes). */
    val frames: Flow<LiveFrame>

    /** Inbound telemetry (operator consumes). */
    val telemetry: StateFlow<TelemetrySnapshot>

    /** Inbound commands (robot consumes). */
    val incomingCommands: Flow<RobotCommand>

    fun connect(config: ConnectionConfig)
    fun disconnect()

    /** Robot side: push a captured camera frame to the network. */
    fun publishFrame(jpeg: ByteArray)

    /** Robot side: push a telemetry snapshot to the network. */
    fun publishTelemetry(snapshot: TelemetrySnapshot)

    /** Operator side: send a drive/aux command to the robot. */
    fun sendCommand(command: RobotCommand)
}
