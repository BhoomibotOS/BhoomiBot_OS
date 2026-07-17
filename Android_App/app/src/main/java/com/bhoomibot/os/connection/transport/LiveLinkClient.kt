// ============================================================================
// LiveLinkClient.kt
// ----------------------------------------------------------------------------
// The transport abstraction for the internet live link. This interface is the
// "socket-shaped" contract: connect, send JSON envelopes, send/receive binary
// video frames. The real implementation is WebSocketLiveLinkClient (OkHttp),
// and tests use FakeLiveLinkClient.
//
// Keeping this an interface is deliberate: LiveLinkRepositoryImpl depends only
// on this contract, so the transport (WebSocket today, maybe WebRTC later) can
// be swapped without touching any screen or ViewModel.
// ============================================================================
package com.bhoomibot.os.connection.transport

import com.bhoomibot.os.connection.model.ConnectionConfig
import com.bhoomibot.os.connection.model.LiveEnvelope
import com.bhoomibot.os.connection.model.LiveFrame
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Low-level transport for the live link. The repository builds on top of this. */
interface LiveLinkClient {
    /** JSON control/telemetry/peer-status envelopes (both directions). */
    val messages: Flow<LiveEnvelope>

    /** Raw binary video frames (ROBOT -> OPERATOR). */
    val frames: Flow<LiveFrame>

    /** Socket lifecycle state. */
    val connectionState: StateFlow<LiveConnectionState>

    fun connect()
    fun disconnect()
    fun send(envelope: LiveEnvelope)
    fun sendFrame(jpeg: ByteArray)

    /** Re-points the underlying socket at a new config without creating a new client. */
    fun updateConfig(config: ConnectionConfig)
}
