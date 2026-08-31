// ============================================================================
// WebSocketLiveLinkClient.kt
// ----------------------------------------------------------------------------
// The ONLY class in the app that talks OkHttp/WebSocket. It implements the
// LiveLinkClient transport contract used by LiveLinkRepositoryImpl.
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
import kotlinx.coroutines.channels.BufferOverflow
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
 * OkHttp-backed [LiveLinkClient]. Opens one WebSocket to the relay using dynamic config.
 */
class WebSocketLiveLinkClient(
    private var config: ConnectionConfig
) : LiveLinkClient {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val _state = MutableStateFlow(LiveConnectionState.IDLE)
    private val _messages = MutableSharedFlow<LiveEnvelope>(extraBufferCapacity = 128)
    private val _frames = MutableSharedFlow<LiveFrame>(
        replay = 0, 
        extraBufferCapacity = 1, 
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private val _lastError = MutableStateFlow<String?>(null)

    private val client = OkHttpClient.Builder()
        .pingInterval(15, TimeUnit.SECONDS)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    @Volatile private var webSocket: WebSocket? = null
    @Volatile private var loopJob: Job? = null

    override val messages: Flow<LiveEnvelope> = _messages.asSharedFlow()
    override val frames: Flow<LiveFrame> = _frames.asSharedFlow()
    override val connectionState: StateFlow<LiveConnectionState> = _state.asStateFlow()
    override val lastError: StateFlow<String?> = _lastError.asStateFlow()

    override fun updateConfig(next: ConnectionConfig) { 
        if (config != next && _state.value != LiveConnectionState.IDLE) {
            disconnect() 
        }
        config = next 
    }

    override fun connect() {
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

    private suspend fun connectionLoop() {
        var attempt = 0
        while (scope.isActive) {
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
                delay(backoff)
                attempt++
            }
        }
    }

    private suspend fun openOnce(): Boolean {
        val opened = CompletableDeferred<Boolean>()
        val closed = CompletableDeferred<Unit>()
        
        val rawUrl = config.serverUrl.trim()
        var url = normalizeToWebSocketUrl(rawUrl)
        
        // ADAPTATION: If using Cloudflare Workers and robotId query param is missing, append it.
        // This ensures compatibility with the new Cloudflare relay structure.
        if (url.contains("workers.dev") && !url.contains("robotId=")) {
            val separator = if (url.contains("?")) "&" else "?"
            url += "${separator}robotId=${config.robotId}"
        }

        Log.d(TAG, "[RELAY] Attempting connection to: $url")
        
        val request = try {
            Request.Builder()
                .url(url)
                .addHeader("User-Agent", "BhoomiBot-Android-OS")
                .build()
        } catch (e: Exception) {
            Log.e(TAG, "[RELAY] URL Build Error: ${e.message}")
            _state.value = LiveConnectionState.ERROR
            return false
        }

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "[RELAY] LINK OPEN: $url")
                _lastError.value = null
                sendHello()
                _state.value = LiveConnectionState.CONNECTED
                opened.complete(true)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                LiveEnvelopeSerializer.decode(text)?.let { _messages.tryEmit(it) }
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                _frames.tryEmit(LiveFrame(bytes.toByteArray(), System.currentTimeMillis()))
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                if (!opened.isCompleted) opened.complete(false)
                if (!closed.isCompleted) closed.complete(Unit)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                val reason = t.message ?: "unknown error"
                Log.e(TAG, "[RELAY] FAILURE: $reason")
                _lastError.value = "Connection failed: $reason"
                if (!opened.isCompleted) opened.complete(false)
                if (!closed.isCompleted) closed.complete(Unit)
            }
        })
        return try {
            opened.await()
        } finally {
            runCatching { closed.await() }
        }
    }

    private fun sendHello() {
        val payload = org.json.JSONObject().apply {
            put("role", config.role.name)
            put("session", config.sessionCode)
        }.toString()
        
        Log.d(TAG, "[RELAY] SENDING HELLO: role=${config.role.name} session=${config.sessionCode}")
        
        send(
            LiveEnvelope(
                type = LiveMessageType.HELLO.code,
                robotId = config.robotId,
                ts = System.currentTimeMillis(),
                payload = payload
            )
        )
    }

    override fun send(envelope: LiveEnvelope) {
        runCatching { webSocket?.send(LiveEnvelopeSerializer.encode(envelope)) }
    }

    override fun sendFrame(jpeg: ByteArray) {
        runCatching { webSocket?.send(jpeg.toByteString()) }
    }
}

private fun normalizeToWebSocketUrl(url: String): String {
    val trimmed = url.trim()
    val lower = trimmed.lowercase()
    return when {
        lower.startsWith("https://") -> "wss://" + trimmed.substring("https://".length)
        lower.startsWith("http://") -> "ws://" + trimmed.substring("http://".length)
        lower.startsWith("ws://") || lower.startsWith("wss://") -> trimmed
        else -> "wss://$trimmed"
    }
}
