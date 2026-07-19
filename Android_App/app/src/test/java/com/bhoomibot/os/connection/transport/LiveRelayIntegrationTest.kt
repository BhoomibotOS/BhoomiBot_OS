// ============================================================================
// LiveRelayIntegrationTest.kt  (DIAGNOSTIC — hits the real internet relay)
// ----------------------------------------------------------------------------
// Proves the Android app's actual transport (WebSocketLiveLinkClient, the exact
// class OperatorLiveViewModel / RobotLiveViewModel drive) really reaches the
// deployed Render relay and that the relay forwards frames between roles.
//
// This is NOT a mock: it opens real WebSockets to wss://bhoomibot-os.onrender.com,
// performs the HELLO handshake, and checks a binary frame published by the ROBOT
// peer is delivered to the OPERATOR peer. Run with --tests on the debug unit test.
// Remove once the connection is confirmed end-to-end.
// ============================================================================
package com.bhoomibot.os.connection.transport

import com.bhoomibot.os.connection.model.ConnectionConfig
import com.bhoomibot.os.model.DeviceRole
import com.bhoomibot.os.connection.transport.LiveConnectionState.CONNECTED
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveRelayIntegrationTest {

    private val relayUrl = "wss://bhoomibot-os.onrender.com"
    private val robotId = "R1"
    private val session = "S1"

    @Test
    fun operatorAndRobotConnectAndRelayForwardsFrames() = runBlocking {
        val operator = WebSocketLiveLinkClient(
            ConnectionConfig(serverUrl = relayUrl, robotId = robotId, sessionCode = session, role = DeviceRole.OPERATOR, autoReconnect = false)
        )
        val robot = WebSocketLiveLinkClient(
            ConnectionConfig(serverUrl = relayUrl, robotId = robotId, sessionCode = session, role = DeviceRole.ROBOT, autoReconnect = false)
        )

        // What the OPERATOR receives from the relay (frames + control messages).
        val frameReceived = CompletableDeferred<ByteArray>()
        val peerStatusSeen = CompletableDeferred<Unit>()
        val opFrameJob = launch { operator.frames.collect { if (!frameReceived.isCompleted) frameReceived.complete(it.jpeg) } }
        val opMsgJob = launch {
            operator.messages.collect { if (it.type == "PEER_STATUS" && !peerStatusSeen.isCompleted) peerStatusSeen.complete(Unit) }
        }

        println("[relay-trace] operator.connect() -> $relayUrl")
        operator.connect()
        println("[relay-trace] robot.connect()    -> $relayUrl")
        robot.connect()

        val opState = withTimeout(30_000) { operator.connectionState.first { it == CONNECTED } }
        val rbState = withTimeout(30_000) { robot.connectionState.first { it == CONNECTED } }
        println("[relay-trace] operator state=$opState  robot state=$rbState")

        // Let the relay process both HELLOs and broadcast PEER_STATUS to each peer.
        kotlinx.coroutines.delay(1500)

        val payload = byteArrayOf(1, 2, 3, 4)
        println("[relay-trace] robot.sendFrame(${payload.size} bytes)")
        robot.sendFrame(payload)

        val received = withTimeout(30_000) { frameReceived.await() }
        println("[relay-trace] operator received relayed frame (${received.size} bytes)")

        withTimeout(30_000) { peerStatusSeen.await() }
        println("[relay-trace] operator received PEER_STATUS from relay (both peers registered)")

        assertTrue("OPERATOR should CONNECT to the live relay", opState == CONNECTED)
        assertTrue("ROBOT should CONNECT to the live relay", rbState == CONNECTED)
        assertTrue("operator should receive the robot's relayed binary frame", received.contentEquals(payload))
        assertTrue("operator should receive a PEER_STATUS envelope from the relay", peerStatusSeen.isCompleted)

        opFrameJob.cancel(); opMsgJob.cancel()
        operator.disconnect(); robot.disconnect()
    }
}
