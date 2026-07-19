package com.bhoomibot.os.connection.repository

import com.bhoomibot.os.connection.model.ConnectionConfig
import com.bhoomibot.os.connection.model.LiveEnvelope
import com.bhoomibot.os.connection.model.LiveFrame
import com.bhoomibot.os.connection.model.LiveMessageType
import com.bhoomibot.os.connection.model.PeerStatus
import com.bhoomibot.os.connection.model.RobotCommand
import com.bhoomibot.os.connection.model.TelemetrySnapshot
import com.bhoomibot.os.connection.protocol.LivePayloads
import com.bhoomibot.os.connection.transport.LiveConnectionState
import com.bhoomibot.os.connection.transport.LiveLinkClient
import com.bhoomibot.os.connection.transport.WebSocketLiveLinkClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * Default [LiveLinkRepository] backed by [WebSocketLiveLinkClient].
 *
 * A single client instance is kept for the repository's lifetime; [connect] just
 * (re)configures it and (re)opens the socket, so the exposed flows stay stable.
 * Inbound JSON messages are fanned out into telemetry / peer-status / commands.
 */
class LiveLinkRepositoryImpl(
    private val client: LiveLinkClient = WebSocketLiveLinkClient(ConnectionConfig()),
    // Production uses a self-owned background scope. Tests pass runTest's scope so they can
    // advance the collector deterministically instead of racing a real Dispatchers.Default thread.
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) : LiveLinkRepository {

    private val _peerStatus = MutableStateFlow(PeerStatus())
    private val _telemetry = MutableStateFlow(TelemetrySnapshot())
    private val _incomingCommands = MutableSharedFlow<RobotCommand>(extraBufferCapacity = 64)
    private var currentRobotId: String = ""

    override val connectionState: kotlinx.coroutines.flow.StateFlow<LiveConnectionState> = client.connectionState
    override val connectionError: kotlinx.coroutines.flow.StateFlow<String?> = client.lastError
    override val frames: Flow<LiveFrame> = client.frames
    override val peerStatus: kotlinx.coroutines.flow.StateFlow<PeerStatus> = _peerStatus.asStateFlow()
    override val telemetry: kotlinx.coroutines.flow.StateFlow<TelemetrySnapshot> = _telemetry.asStateFlow()
    override val incomingCommands: Flow<RobotCommand> = _incomingCommands.asSharedFlow()

    override fun connect(config: ConnectionConfig) {
        android.util.Log.d(
            "BhoomiBotRelay",
            "[PIPELINE] LiveLinkRepositoryImpl.connect() ENTERED config=serverUrl='${config.serverUrl}' robotId='${config.robotId}' session='${config.sessionCode}' role=${config.role}"
        )
        currentRobotId = config.robotId // remembered so outbound envelopes can be stamped
        // Restart the fan-out collector for this session (cancel any previous one).
        collectorJob?.cancel()
        collectorJob = scope.launch {
            // Single inbound stream of JSON envelopes -> demultiplex by type into the
            // right role-specific flow. Anything unrecognized (HELLO, PING, ERROR...)
            // is ignored here via the `else` branch. Undecodable payloads are dropped.
            client.messages.collect { env ->
                android.util.Log.d(
                    "BhoomiBotRelay",
                    "[PIPELINE] received envelope type=${env.type} robotId=${env.robotId}"
                )
                when (LiveMessageType.from(env.type)) {
                    LiveMessageType.TELEMETRY ->
                        LivePayloads.decodeTelemetry(env.payload)?.let { _telemetry.value = it }
                    LiveMessageType.PEER_STATUS ->
                        LivePayloads.decodePeerStatus(env.payload)?.let { _peerStatus.value = it }
                    LiveMessageType.COMMAND ->
                        LivePayloads.decodeCommand(env.payload)?.let { _incomingCommands.tryEmit(it) }
                    else -> Unit
                }
            }
        }
        // Push the new config into the (reused) client, THEN open the socket. Order
        // matters: updateConfig must land before connect() so HELLO uses this session.
        android.util.Log.d("BhoomiBotRelay", "[PIPELINE] calling client.updateConfig() -> client.connect()")
        client.updateConfig(config)
        client.connect()
        android.util.Log.d("BhoomiBotRelay", "[PIPELINE] LiveLinkRepositoryImpl.connect() COMPLETED")
    }

    override fun disconnect() {
        collectorJob?.cancel()
        collectorJob = null
        client.disconnect()
        _peerStatus.value = PeerStatus()
        _telemetry.value = TelemetrySnapshot()
    }

    override fun publishFrame(jpeg: ByteArray) = client.sendFrame(jpeg)

    override fun publishTelemetry(snapshot: TelemetrySnapshot) {
        client.send(
            LiveEnvelope(
                type = LiveMessageType.TELEMETRY.code,
                robotId = currentRobotId,
                ts = System.currentTimeMillis(),
                payload = LivePayloads.encodeTelemetry(snapshot)
            )
        )
    }

    override fun sendCommand(command: RobotCommand) {
        client.send(
            LiveEnvelope(
                type = LiveMessageType.COMMAND.code,
                robotId = currentRobotId,
                ts = System.currentTimeMillis(),
                payload = LivePayloads.encodeCommand(command)
            )
        )
    }

    private var collectorJob: Job? = null
}
