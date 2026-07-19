// ============================================================================
// FakeLiveLinkClient.kt (test double)
// ----------------------------------------------------------------------------
// A hand-written stand-in for WebSocketLiveLinkClient used by
// LiveLinkRepositoryTest. It implements the same LiveLinkClient interface but
// keeps everything in memory: no OkHttp, no real socket, no network.
//   - It RECORDS outbound traffic (connectCalls, sentEnvelopes, sentFrames) so
//     tests can assert what the repository sent.
//   - It EXPOSES emitMessage/emitFrame so tests can simulate the relay pushing
//     inbound traffic and verify the repository's fan-out logic.
// This is exactly why LiveLinkClient is an interface (see LiveLinkClient.kt).
// ============================================================================
package com.bhoomibot.os.connection.repository

import com.bhoomibot.os.connection.model.ConnectionConfig
import com.bhoomibot.os.connection.model.LiveEnvelope
import com.bhoomibot.os.connection.model.LiveFrame
import com.bhoomibot.os.connection.transport.LiveConnectionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-memory [LiveLinkClient] for unit-testing [LiveLinkRepositoryImpl] without a socket. */
class FakeLiveLinkClient : com.bhoomibot.os.connection.transport.LiveLinkClient {
    var connectCalls = 0
    var disconnectCalls = 0
    val sentFrames = mutableListOf<ByteArray>()
    val sentEnvelopes = mutableListOf<LiveEnvelope>()
    var latestConfig: ConnectionConfig? = null

    private val _state = MutableStateFlow(LiveConnectionState.IDLE)
    private val _messages = MutableSharedFlow<LiveEnvelope>(extraBufferCapacity = 64)
    private val _frames = MutableSharedFlow<LiveFrame>(extraBufferCapacity = 64)
    private val _lastError = MutableStateFlow<String?>(null)

    override val messages: Flow<LiveEnvelope> = _messages.asSharedFlow()
    override val frames: Flow<LiveFrame> = _frames.asSharedFlow()
    override val connectionState: StateFlow<LiveConnectionState> = _state.asStateFlow()
    override val lastError: StateFlow<String?> = _lastError.asStateFlow()

    override fun connect() { connectCalls++; _state.value = LiveConnectionState.CONNECTED }
    override fun disconnect() { disconnectCalls++; _state.value = LiveConnectionState.IDLE }
    // The real socket saves this config for its next connection; retaining it here lets tests
    // verify the same repository-to-transport handoff without opening a network connection.
    override fun updateConfig(config: ConnectionConfig) { latestConfig = config }
    override fun send(envelope: LiveEnvelope) { sentEnvelopes.add(envelope) }
    override fun sendFrame(jpeg: ByteArray) { sentFrames.add(jpeg) }

    // Test helpers: simulate inbound traffic from the relay.
    fun emitMessage(e: LiveEnvelope) = _messages.tryEmit(e)
    fun emitFrame(f: LiveFrame) = _frames.tryEmit(f)
}
