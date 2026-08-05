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
import android.util.Base64
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
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) : LiveLinkRepository {

    private val _peerStatus = MutableStateFlow(PeerStatus())
    private val _telemetry = MutableStateFlow(TelemetrySnapshot())
    private val _incomingCommands = MutableSharedFlow<RobotCommand>(extraBufferCapacity = 64)
    private val _frames = MutableSharedFlow<LiveFrame>(extraBufferCapacity = 64)
    private val _alerts = MutableSharedFlow<Pair<String, String>>(extraBufferCapacity = 8)
    private var currentRobotId: String = ""
    private var currentServerUrl: String? = null

    override val connectionState: kotlinx.coroutines.flow.StateFlow<LiveConnectionState> = client.connectionState
    override val connectionError: kotlinx.coroutines.flow.StateFlow<String?> = client.lastError
    override val frames: Flow<LiveFrame> = _frames.asSharedFlow()
    override val peerStatus: kotlinx.coroutines.flow.StateFlow<PeerStatus> = _peerStatus.asStateFlow()
    override val telemetry: kotlinx.coroutines.flow.StateFlow<TelemetrySnapshot> = _telemetry.asStateFlow()
    override val incomingCommands: Flow<RobotCommand> = _incomingCommands.asSharedFlow()
    override val alerts: Flow<Pair<String, String>> = _alerts.asSharedFlow()

    init {
        // AI-Fix: Merged Frame Logic. We must collect from both binary and JSON paths.
        scope.launch {
            client.frames.collect { binaryFrame ->
                _frames.emit(binaryFrame)
            }
        }
    }

    override fun connect(config: ConnectionConfig) {
        currentRobotId = config.robotId 
        currentServerUrl = config.serverUrl

        collectorJob?.cancel()
        collectorJob = scope.launch {
            client.messages.collect { env ->
                when (LiveMessageType.from(env.type)) {
                    LiveMessageType.TELEMETRY ->
                        LivePayloads.decodeTelemetry(env.payload)?.let { _telemetry.value = it }
                    LiveMessageType.PEER_STATUS ->
                        LivePayloads.decodePeerStatus(env.payload)?.let { _peerStatus.value = it }
                    LiveMessageType.COMMAND ->
                        LivePayloads.decodeCommand(env.payload)?.let { _incomingCommands.tryEmit(it) }
                    LiveMessageType.ALERT ->
                        LivePayloads.decodeAlert(env.payload)?.let { _alerts.tryEmit(it) }
                    LiveMessageType.VIDEO_FRAME ->
                        env.payload?.let { base64 ->
                            // AI-Fix: Extract video from JSON for Render compatibility
                            val jpeg = Base64.decode(base64, Base64.NO_WRAP)
                            _frames.tryEmit(LiveFrame(jpeg, env.ts))
                        }
                    else -> Unit
                }
            }
        }
        client.updateConfig(config)
        client.connect()
    }

    override fun disconnect() {
        collectorJob?.cancel()
        collectorJob = null
        client.disconnect()
        _peerStatus.value = PeerStatus()
        _telemetry.value = TelemetrySnapshot()
    }

    override fun publishFrame(jpeg: ByteArray) {
        val serverUrl = currentServerUrl ?: ""
        // Use Base64-JSON for secure internet relays (Render/Cloud proxies)
        if (serverUrl.startsWith("wss://") || serverUrl.contains("onrender.com")) {
            val base64 = Base64.encodeToString(jpeg, Base64.NO_WRAP)
            client.send(
                LiveEnvelope(
                    type = LiveMessageType.VIDEO_FRAME.code,
                    robotId = currentRobotId,
                    ts = System.currentTimeMillis(),
                    payload = base64
                )
            )
        } else {
            // Hotspot Mode: Use high-speed raw binary
            client.sendFrame(jpeg)
        }
    }


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

    override fun publishAlert(message: String, severity: String) {
        client.send(
            LiveEnvelope(
                type = LiveMessageType.ALERT.code,
                robotId = currentRobotId,
                ts = System.currentTimeMillis(),
                payload = LivePayloads.encodeAlert(message, severity)
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
