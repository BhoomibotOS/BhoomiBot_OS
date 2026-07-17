// ============================================================================
// LiveLinkRepositoryTest.kt
// ----------------------------------------------------------------------------
// Verifies LiveLinkRepositoryImpl's behavior against a FakeLiveLinkClient, with
// no real socket. Two directions are covered:
//   - INBOUND: an emitted envelope is decoded and routed to the correct flow
//     (telemetry/command/peer-status). Tests use flow.first { ... } to suspend
//     until the expected value arrives, which is why they run in runTest.
//   - OUTBOUND: connect/disconnect/publish/send call through to the client and
//     encode the right envelope type.
// ============================================================================
package com.bhoomibot.os.connection.repository

import com.bhoomibot.os.connection.model.ConnectionConfig
import com.bhoomibot.os.connection.model.LiveEnvelope
import com.bhoomibot.os.connection.model.LiveFrame
import com.bhoomibot.os.connection.model.LiveMessageType
import com.bhoomibot.os.connection.model.PeerStatus
import com.bhoomibot.os.connection.model.RobotCommand
import com.bhoomibot.os.connection.model.TelemetrySnapshot
import com.bhoomibot.os.connection.protocol.LivePayloads
import com.bhoomibot.os.model.DeviceRole
import com.bhoomibot.os.model.DriveCommand
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveLinkRepositoryTest {

    private val config = ConnectionConfig(robotId = "R1", sessionCode = "S1", role = DeviceRole.ROBOT)

    @Test fun connectInvokesClientAndReportsState() = runTest {
        val fake = FakeLiveLinkClient()
        val repo = LiveLinkRepositoryImpl(fake)
        repo.connect(config)
        assertEquals(1, fake.connectCalls)
        assertEquals(com.bhoomibot.os.connection.transport.LiveConnectionState.CONNECTED, repo.connectionState.value)
    }

    @Test fun inboundTelemetryRoutesToState() = runTest {
        val fake = FakeLiveLinkClient()
        val repo = LiveLinkRepositoryImpl(fake)
        repo.connect(config)
        fake.emitMessage(
            LiveEnvelope(
                type = LiveMessageType.TELEMETRY.code,
                payload = LivePayloads.encodeTelemetry(TelemetrySnapshot(batteryPercent = 42))
            )
        )
        val t = repo.telemetry.first { it.batteryPercent == 42 }
        assertEquals(42, t.batteryPercent)
    }

    @Test fun inboundCommandRoutesToFlow() = runTest {
        val fake = FakeLiveLinkClient()
        val repo = LiveLinkRepositoryImpl(fake)
        repo.connect(config)
        fake.emitMessage(
            LiveEnvelope(
                type = LiveMessageType.COMMAND.code,
                payload = LivePayloads.encodeCommand(RobotCommand(drive = DriveCommand.LEFT, speedPercent = 25))
            )
        )
        val c = repo.incomingCommands.first { it.drive == DriveCommand.LEFT }
        assertEquals(25, c.speedPercent)
    }

    @Test fun inboundPeerStatusRoutesToState() = runTest {
        val fake = FakeLiveLinkClient()
        val repo = LiveLinkRepositoryImpl(fake)
        repo.connect(config)
        fake.emitMessage(
            LiveEnvelope(
                type = LiveMessageType.PEER_STATUS.code,
                payload = LivePayloads.encodePeerStatus(PeerStatus(robotOnline = true, operatorOnline = true))
            )
        )
        val p = repo.peerStatus.first { it.robotOnline && it.operatorOnline }
        assertTrue(p.robotOnline)
        assertTrue(p.operatorOnline)
    }

    @Test fun publishFrameSendsToClient() = runTest {
        val fake = FakeLiveLinkClient()
        val repo = LiveLinkRepositoryImpl(fake)
        repo.connect(config)
        val bytes = byteArrayOf(1, 2, 3)
        repo.publishFrame(bytes)
        assertEquals(1, fake.sentFrames.size)
        assertTrue(bytes.contentEquals(fake.sentFrames[0]))
    }

    @Test fun publishTelemetryEncodesEnvelope() = runTest {
        val fake = FakeLiveLinkClient()
        val repo = LiveLinkRepositoryImpl(fake)
        repo.connect(config)
        repo.publishTelemetry(TelemetrySnapshot(batteryPercent = 7))
        val env = fake.sentEnvelopes.first { it.type == LiveMessageType.TELEMETRY.code }
        assertEquals(7, LivePayloads.decodeTelemetry(env.payload)?.batteryPercent)
    }

    @Test fun sendCommandEncodesEnvelope() = runTest {
        val fake = FakeLiveLinkClient()
        val repo = LiveLinkRepositoryImpl(fake)
        repo.connect(config)
        repo.sendCommand(RobotCommand(drive = DriveCommand.RIGHT, speedPercent = 10))
        val env = fake.sentEnvelopes.first { it.type == LiveMessageType.COMMAND.code }
        assertEquals(DriveCommand.RIGHT, LivePayloads.decodeCommand(env.payload)?.drive)
    }

    @Test fun disconnectInvokesClient() = runTest {
        val fake = FakeLiveLinkClient()
        val repo = LiveLinkRepositoryImpl(fake)
        repo.connect(config)
        repo.disconnect()
        assertEquals(1, fake.disconnectCalls)
        assertEquals(com.bhoomibot.os.connection.transport.LiveConnectionState.IDLE, repo.connectionState.value)
    }
}
