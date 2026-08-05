// ============================================================================
// WebSocketLiveLinkClient.kt
// ----------------------------------------------------------------------------
// The ONLY class in the app that talks OkHttp/WebSocket. It implements the
// LiveLinkClient transport contract used by LiveLinkRepositoryImpl.
//
// Responsibilities:
//   - Open exactly one WebSocket to the relay server for the client's lifetime.
//   - On open, send the HELLO handshake (role + session code) so the relay can
//     pair this phone with its peer in the same session.
//   - Fan inbound traffic into two streams: text -> JSON envelopes (control /
//     telemetry / peer-status), binary -> raw jpeg video frames.
//   - Auto-reconnect with capped exponential backoff when the socket drops.
//
// Threading: everything runs on a private Dispatchers.IO coroutine scope. The
// OkHttp WebSocketListener callbacks fire on OkHttp's own threads and simply
// push into flows / complete deferreds, so they stay non-blocking.
// ============================================================================
package com.bhoomibot.os.connection.transport

import com.bhoomibot.os.connection.model.ConnectionConfig
import com.bhoomibot.os.connection.model.LiveEnvelope
import com.bhoomibot.os.connection.model.LiveFrame
import com.bhoomibot.os.connection.model.LiveMessageType
import com.bhoomibot.os.connection.protocol.LiveEnvelopeSerializer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import android.util.Log
import java.net.URI
import java.util.concurrent.TimeUnit
import kotlin.math.min

/** Tag for all relay [Log] lines so they're easy to filter in logcat. */
private const val TAG = "BhoomiBotRelay"

/**
 * OkHttp-backed [LiveLinkClient]. Opens one WebSocket to the relay, sends the HELLO
 * handshake, then relays JSON envelopes and binary video frames. If the socket drops and
 * [ConnectionConfig.autoReconnect] is on, it retries with capped exponential backoff.
 *
 * This is the only class that knows about OkHttp; the repository above it stays
 * transport-agnostic, so a WebRTC transport could be dropped in later behind the
 * same [LiveLinkClient] interface.
 */
class WebSocketLiveLinkClient(
    private var config: ConnectionConfig
) : LiveLinkClient {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _state = MutableStateFlow(LiveConnectionState.IDLE)
    // SharedFlows (not StateFlows) because messages/frames are events, not state:
    // each one should be delivered once, not re-replayed to new collectors. The
    // extra buffer absorbs bursts so a momentarily-slow collector doesn't block
    // the OkHttp callback thread (tryEmit drops rather than suspends).
    private val _messages = MutableSharedFlow<LiveEnvelope>(extraBufferCapacity = 128)
    private val _frames = MutableSharedFlow<LiveFrame>(extraBufferCapacity = 64)
    private val _lastError = MutableStateFlow<String?>(null)

    private val client = OkHttpClient.Builder()
        .pingInterval(15, TimeUnit.SECONDS) // keepalive through mobile NATs
        // Render's free tier spins the service down after inactivity, so the first
        // WebSocket handshake after a cold start can take 30-60s. OkHttp's 10s
        // default connect/read timeout would abort that handshake and surface a
        // spurious ERROR/RECONNECTING on first connect. 30s lets attempt #1 ride
        // out the wake-up; autoReconnect covers anything longer.
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    @Volatile private var webSocket: WebSocket? = null
    @Volatile private var loopJob: Job? = null

    override val messages: Flow<LiveEnvelope> = _messages.asSharedFlow()
    override val frames: Flow<LiveFrame> = _frames.asSharedFlow()
    override val connectionState: StateFlow<LiveConnectionState> = _state.asStateFlow()
    override val lastError: StateFlow<String?> = _lastError.asStateFlow()

    // Re-point at a new config. 
    override fun updateConfig(next: ConnectionConfig) { 
        if (config != next && _state.value != LiveConnectionState.IDLE) {
            disconnect() 
        }
        config = next 
    }

    override fun connect() {
        Log.d(TAG, "[RELAY] connect() called — loop already active? ${loopJob?.isActive == true}")
        // Guard against double-connect: if the loop is already running, do nothing.
        if (loopJob?.isActive == true) return
        loopJob = scope.launch { connectionLoop() }
    }

    override fun disconnect() {
        loopJob?.cancel()
        loopJob = null
        runCatching { webSocket?.close(1000, null) }
        webSocket = null
        _state.value = LiveConnectionState.IDLE
    }

    /**
     * The reconnect state machine. Runs as a single coroutine for as long as the
     * client is connected. Each pass opens the socket once and blocks until it
     * closes; then it decides whether to give up (ERROR) or wait and retry.
     *
     * `attempt` is the consecutive-failure counter: it resets to 0 on every
     * successful open, and drives the backoff delay while failures pile up.
     */
    private suspend fun connectionLoop() {
        var attempt = 0
        while (scope.isActive) {
            // First try shows CONNECTING; any retry shows RECONNECTING.
            _state.value = if (attempt == 0) LiveConnectionState.CONNECTING
            else LiveConnectionState.RECONNECTING
            
            val opened = openOnce()
            
            if (opened) {
                attempt = 0
            } else {
                if (!config.autoReconnect) {
                    _state.value = LiveConnectionState.ERROR
                    return
                }
                val backoff = min(1000L * (1 shl attempt.coerceAtMost(5)), 30_000L)
                _state.value = LiveConnectionState.RECONNECTING
                delay(backoff)
                attempt++
            }
        }
    }

    /**
     * Opens the socket and suspends until it closes. Returns true if the socket
     * successfully opened (HELLO sent, CONNECTED reached), false if it failed.
     *
     * Two CompletableDeferreds bridge OkHttp's callback world to this suspend fn:
     *   - `opened`: completed true from onOpen, or false if it fails/closes first.
     *              We `await` this to learn the open result and return it.
     *   - `closed`: completed when the socket closes/fails, so the `finally` block
     *              keeps this coroutine parked for the socket's whole lifetime.
     *              That is what makes the connectionLoop wait here until a drop.
     */
    private suspend fun openOnce(): Boolean {
        val opened = CompletableDeferred<Boolean>()
        val closed = CompletableDeferred<Unit>()
        // Accept a user-facing http(s):// relay address too: normalize to ws(s)://
        // so a relay pasted as its https site URL still opens a valid WebSocket.
        // This makes the transport resilient even if a caller forgot to normalize.
        val rawUrl = config.serverUrl.trim()
        val url = normalizeToWebSocketUrl(rawUrl)
        val parsed = runCatching { URI(url) }.getOrNull()
        Log.d(TAG, "[RELAY] Attempting relay connection")
        Log.d(TAG, "[RELAY] final URL=$url (raw='$rawUrl') protocol=${parsed?.scheme} host=${parsed?.host} path=${parsed?.path ?: "/"}")
        // Validate the scheme up front. A bad URL is a terminal config error, not
        // something retrying will fix, so go straight to ERROR and bail.
        if (!url.startsWith("ws://") && !url.startsWith("wss://")) {
            Log.e(TAG, "[RELAY] ERROR: URL scheme is not ws(s):// — cannot open WebSocket: '$url'")
            _state.value = LiveConnectionState.ERROR
            return false
        }
        val request = try {
            Request.Builder().url(url).build()
        } catch (_: IllegalArgumentException) {
            // Malformed URL that passed the prefix check (e.g. "ws://").
            Log.e(TAG, "[RELAY] ERROR: malformed URL: '$url'")
            _state.value = LiveConnectionState.ERROR
            return false
        }
        Log.d(TAG, "[RELAY] Opening WebSocket — headers=${request.headers}")
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "[RELAY] CONNECTED url=$url")
                _lastError.value = null
                sendHello() // announce role + session so the relay can pair us
                _state.value = LiveConnectionState.CONNECTED
                opened.complete(true)
            }

            // Text frame -> a JSON control/telemetry envelope. Undecodable text is
            // dropped silently (decode returns null) rather than crashing the link.
            override fun onMessage(webSocket: WebSocket, text: String) {
                LiveEnvelopeSerializer.decode(text)?.let { _messages.tryEmit(it) }
            }

            // Binary frame -> a raw jpeg video frame. Video bytes intentionally
            // travel OUTSIDE the JSON envelope for efficiency; timestamp is set on
            // arrival here since the wire carries no per-frame metadata.
            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                _frames.tryEmit(LiveFrame(bytes.toByteArray(), System.currentTimeMillis()))
            }

            // Peer began a graceful close: acknowledge it to complete the handshake.
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "[RELAY] CLOSING code=$code reason='$reason'")
                webSocket.close(1000, null)
            }

            // Socket fully closed. Unblock both deferreds (guarding against having
            // already completed them) so openOnce() returns and the loop proceeds.
            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "[RELAY] CLOSED code=$code reason='$reason'")
                if (!opened.isCompleted) opened.complete(false)
                if (!closed.isCompleted) closed.complete(Unit)
            }

            // Network/protocol error: treat exactly like a close so backoff kicks in.
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                val reason = t.message ?: "unknown error"
                Log.e(TAG, "[RELAY] ERROR url=$url msg=$reason responseCode=${response?.code}", t)
                _lastError.value = "WS failed: $reason${if (response != null) " (HTTP ${response.code})" else ""}"
                if (!opened.isCompleted) opened.complete(false)
                if (!closed.isCompleted) closed.complete(Unit)
            }
        })
        return try {
            opened.await() // returns as soon as we know open succeeded/failed
        } finally {
            // Park here until the socket actually closes, so connectionLoop treats
            // one openOnce() call as "the whole life of this connection".
            runCatching { closed.await() }
        }
    }

    // The first message on every connection.
    private fun sendHello() {
        val payload = org.json.JSONObject().apply {
            put("role", config.role.name)
            put("session", config.sessionCode)
        }.toString()
        send(
            LiveEnvelope(
                type = LiveMessageType.HELLO.code,
                robotId = config.robotId,
                ts = System.currentTimeMillis(),
                payload = payload
            )
        )
    }

    // Send a JSON envelope as a text frame. runCatching swallows the case where the
    // socket is momentarily null/closed — the caller shouldn't crash on a dropped link.
    override fun send(envelope: LiveEnvelope) {
        runCatching { webSocket?.send(LiveEnvelopeSerializer.encode(envelope)) }
    }

    // Send jpeg bytes as a binary frame (outside the JSON envelope).
    override fun sendFrame(jpeg: ByteArray) {
        runCatching { webSocket?.send(jpeg.toByteString()) }
    }
}

/**
 * Converts a user-typed relay address into one OkHttp can open. A pasted https://
 * site URL becomes wss://; http:// becomes ws://. Already-correct ws(s):// stays put.
 * Mirrors the UI's [com.bhoomibot.os.feature.connection.ConnectionOptionsUiState] normalization
 * so the transport never silently rejects a valid relay just because it was given https://.
 */
private fun normalizeToWebSocketUrl(url: String): String {
    val trimmed = url.trim()
    val lower = trimmed.lowercase()
    return when {
        lower.startsWith("https://") -> "wss://" + trimmed.substring("https://".length)
        lower.startsWith("http://") -> "ws://" + trimmed.substring("http://".length)
        else -> trimmed
    }
}
