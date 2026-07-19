// ============================================================================
// WebSocketLiveLinkClientConnectTest.kt
// ----------------------------------------------------------------------------
// Runtime proof that OperatorLiveViewModel / RobotLiveViewModel don't just flip
// a UI flag when they call repository.connect(): the underlying WebSocket client
// actually dials a real socket and performs the HELLO handshake.
//
// We stand up a local MockWebServer, point the client at it, and assert:
//   1. the client's connectionState really reaches CONNECTED (a socket opened), and
//   2. the server actually receives the HELLO envelope the client sends on open.
// A second test proves a dead relay is genuinely attempted and surfaced as ERROR.
//
// This runs on the JVM (no device/emulator): WebSocketLiveLinkClient only depends
// on OkHttp + coroutines, not on Android.
// ============================================================================
package com.bhoomibot.os.connection.transport

import com.bhoomibot.os.connection.model.ConnectionConfig
import com.bhoomibot.os.connection.transport.LiveConnectionState
import com.bhoomibot.os.model.DeviceRole
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WebSocketLiveLinkClientConnectTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() { server = MockWebServer() }

    @After
    fun tearDown() { server.shutdown() }

    @Test
    fun connectOpensRealSocketAndSendsHello() = runBlocking {
        // The relay side: capture the first text frame the client sends on open.
        val helloReceived = CompletableDeferred<String>()
        server.enqueue(
            MockResponse().withWebSocketUpgrade(object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) = Unit
                override fun onMessage(webSocket: WebSocket, text: String) {
                    helloReceived.complete(text)
                }
            })
        )
        // The client's reconnect loop opens a 2nd socket once the 1st is closed; give
        // that attempt a quick non-WebSocket response so it fails fast and the loop
        // exits (autoReconnect=false) instead of blocking MockWebServer's queue.
        server.enqueue(MockResponse().setResponseCode(200))
        server.start()

        // Point the client at the local server (http:// -> ws://).
        val wsUrl = server.url("/").toString().replace("http", "ws")
        val config = ConnectionConfig(
            serverUrl = wsUrl,
            robotId = "R1",
            sessionCode = "S1",
            role = DeviceRole.OPERATOR,
            autoReconnect = false // stop the loop after the first open so the test settles
        )
        val client = WebSocketLiveLinkClient(config)

        client.connect()

        // The client must really reach CONNECTED (a socket was opened), not just spin.
        val finalState = withTimeout(10_000) {
            client.connectionState.first { it == LiveConnectionState.CONNECTED }
        }
        // And the relay must actually receive the HELLO handshake the client sends on open.
        val hello = withTimeout(10_000) { helloReceived.await() }

        assertTrue(
            "expected CONNECTED after opening a real socket, got ${client.connectionState.value}",
            finalState == LiveConnectionState.CONNECTED
        )
        // The client sends the HELLO wrapped in a LiveEnvelope; role/session live inside
        // the (escaped) payload string, so assert on the values that survive encoding.
        assertTrue("HELLO handshake should be sent on open", hello.contains("HELLO"))
        assertTrue("HELLO envelope should carry a payload", hello.contains("payload"))
        assertTrue("HELLO should identify as OPERATOR", hello.contains("OPERATOR"))
        assertTrue("HELLO should carry the session code S1", hello.contains("S1"))

        client.disconnect()
    }

    @Test
    fun connectToUnreachableRelayReportsError() = runBlocking {
        // A dead endpoint: the client must really attempt the connection and surface
        // the failure as ERROR rather than silently doing nothing.
        val config = ConnectionConfig(
            serverUrl = "ws://localhost:1/",
            robotId = "R1",
            sessionCode = "S1",
            role = DeviceRole.OPERATOR,
            autoReconnect = false
        )
        val client = WebSocketLiveLinkClient(config)
        client.connect()

        val state = withTimeout(10_000) {
            client.connectionState.first { it == LiveConnectionState.ERROR }
        }
        assertTrue("unreachable relay should surface ERROR, got $state", state == LiveConnectionState.ERROR)

        client.disconnect()
    }
}
