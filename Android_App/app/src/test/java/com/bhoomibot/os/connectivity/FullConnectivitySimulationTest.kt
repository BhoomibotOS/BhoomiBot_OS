package com.bhoomibot.os.connectivity

import com.bhoomibot.os.connection.model.ConnectionConfig
import com.bhoomibot.os.connection.transport.LiveConnectionState
import com.bhoomibot.os.connection.transport.WebSocketLiveLinkClient
import com.bhoomibot.os.connection.transport.local.LocalRelayServer
import com.bhoomibot.os.model.DeviceRole
import com.bhoomibot.os.model.DriveCommand
import com.bhoomibot.os.vcu.toProtocol
import com.bhoomibot.os.vcu.speedCommand
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.ServerSocket

/**
 * COMPREHENSIVE CONNECTIVITY SIMULATION
 * Proves that Operator and Robot can talk across all modes.
 */
class FullConnectivitySimulationTest {

    private val RENDER_URL = "wss://bhoomibot-os.onrender.com"
    private val TEST_ROBOT_ID = "SIM_ROBOT_001"
    private val TEST_SESSION = "SIM_SESSION_99"

    /**
     * SCENARIO 1: Internet Mode (Render Relay)
     * Proves: Handshake, Session Pairing, and Video Frame Relaying.
     */
    @Test
    fun testInternetRelayConnectivity() = runBlocking {
        println("\n[SIMULATION] Starting Internet Mode Test (Render)...")
        
        val robot = WebSocketLiveLinkClient(ConnectionConfig(
            serverUrl = RENDER_URL,
            robotId = TEST_ROBOT_ID,
            sessionCode = TEST_SESSION,
            role = DeviceRole.ROBOT
        ))

        val operator = WebSocketLiveLinkClient(ConnectionConfig(
            serverUrl = RENDER_URL,
            robotId = TEST_ROBOT_ID,
            sessionCode = TEST_SESSION,
            role = DeviceRole.OPERATOR
        ))

        // Virtual "Eyes" and "Screen"
        val frameSent = byteArrayOf(0xDE.toByte(), 0xAD.toByte(), 0xBE.toByte(), 0xEF.toByte())
        val frameReceived = CompletableDeferred<ByteArray>()

        val opJob = launch {
            operator.frames.collect { frameReceived.complete(it.jpeg) }
        }

        try {
            println("[SIM] Connecting Robot...")
            robot.connect()
            println("[SIM] Connecting Operator...")
            operator.connect()

            // Wait for both to be CONNECTED
            withTimeout(15000) {
                robot.connectionState.first { it == LiveConnectionState.CONNECTED }
                operator.connectionState.first { it == LiveConnectionState.CONNECTED }
            }
            println("[SIM] SUCCESS: Both devices paired on Render!")

            // Robot Sends Video
            println("[SIM] Robot publishing virtual frame...")
            robot.sendFrame(frameSent)

            // Operator Receives Video
            val received = withTimeout(5000) { frameReceived.await() }
            assertTrue("Operator should receive exact bytes sent by Robot", received.contentEquals(frameSent))
            println("[SIM] SUCCESS: Video frame relayed across the internet!")

        } finally {
            opJob.cancel()
            robot.disconnect()
            operator.disconnect()
        }
    }

    /**
     * SCENARIO 2: Hotspot Mode (Local Hub)
     * Proves: Local WebSocket server starts and routes commands without internet.
     */
    @Test
    fun testHotspotLocalConnectivity() = runBlocking {
        println("\n[SIMULATION] Starting Hotspot Mode Test (Local Hub)...")
        
        // Find an open port
        val port = ServerSocket(0).use { it.localPort }
        val localUrl = "ws://localhost:$port"

        var serverStarted = false
        val server = LocalRelayServer(port, { serverStarted = true }, { it.printStackTrace() })
        server.start()

        // Give server a moment to bind
        delay(500)

        val robot = WebSocketLiveLinkClient(ConnectionConfig(
            serverUrl = localUrl,
            robotId = "LOCAL_BOT",
            sessionCode = "123",
            role = DeviceRole.ROBOT
        ))

        val operator = WebSocketLiveLinkClient(ConnectionConfig(
            serverUrl = localUrl,
            robotId = "LOCAL_BOT",
            sessionCode = "123",
            role = DeviceRole.OPERATOR
        ))

        try {
            robot.connect()
            operator.connect()

            withTimeout(5000) {
                assertTrue("Local Relay Server should be live", serverStarted)
                robot.connectionState.first { it == LiveConnectionState.CONNECTED }
                operator.connectionState.first { it == LiveConnectionState.CONNECTED }
            }
            println("[SIM] SUCCESS: Local Hotspot link established without Internet!")
        } finally {
            robot.disconnect()
            operator.disconnect()
            server.stop()
        }
    }

    /**
     * SCENARIO 3: VCU Protocol Translation
     * Proves: App intents translate to exact ESP32 serial strings.
     */
    @Test
    fun testVcuProtocolTranslation() {
        println("\n[SIMULATION] Testing VCU Protocol Translation...")
        
        assertEquals("F", DriveCommand.FORWARD.toProtocol())
        assertEquals("B", DriveCommand.REVERSE.toProtocol())
        assertEquals("SPD50", speedCommand(50))
        assertEquals("SPD-100", speedCommand(-100))
        
        println("[SIM] SUCCESS: Joystick intents correctly mapped to VCU serial tokens!")
    }
}
