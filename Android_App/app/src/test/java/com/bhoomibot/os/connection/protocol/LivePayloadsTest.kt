// ============================================================================
// LivePayloadsTest.kt
// ----------------------------------------------------------------------------
// Pure-JVM unit tests for the payload codecs carried inside the envelope
// (telemetry, command, peer-status). The key cases to protect: round-trip
// equality, the command's tri-state optionals (pto/lights) surviving as null
// vs true/false, and null-in -> null-out for absent payloads.
// ============================================================================
package com.bhoomibot.os.connection.protocol

import com.bhoomibot.os.connection.model.PeerStatus
import com.bhoomibot.os.connection.model.RobotCommand
import com.bhoomibot.os.connection.model.TelemetrySnapshot
import com.bhoomibot.os.model.DriveCommand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LivePayloadsTest {

    @Test
    fun `telemetry round-trips`() {
        val t = TelemetrySnapshot(
            isOnline = true, batteryPercent = 73, mode = "Manual",
            mission = "Spray", gpsStatus = "Locked", cameraStatus = "Ready", aiStatus = "Active"
        )
        val back = LivePayloads.decodeTelemetry(LivePayloads.encodeTelemetry(t))!!
        assertEquals(t, back)
    }

    @Test
    fun `command round-trips including optionals`() {
        val c = RobotCommand(drive = DriveCommand.LEFT, speedPercent = 40, emergencyStop = false, pto = true, lights = false)
        val back = LivePayloads.decodeCommand(LivePayloads.encodeCommand(c))!!
        assertEquals(DriveCommand.LEFT, back.drive)
        assertEquals(40, back.speedPercent)
        assertEquals(true, back.pto)
        assertEquals(false, back.lights)
        assertFalse(back.emergencyStop)
    }

    @Test
    fun `command with null optionals decodes to null`() {
        val c = RobotCommand(drive = DriveCommand.STOP)
        val back = LivePayloads.decodeCommand(LivePayloads.encodeCommand(c))!!
        assertNull(back.pto)
        assertNull(back.lights)
        assertEquals(DriveCommand.STOP, back.drive)
    }

    @Test
    fun `peer status round-trips`() {
        val p = PeerStatus(robotOnline = true, operatorOnline = false)
        val back = LivePayloads.decodePeerStatus(LivePayloads.encodePeerStatus(p))!!
        assertTrue(back.robotOnline)
        assertFalse(back.operatorOnline)
    }

    @Test
    fun `null payload decodes to null`() {
        assertNull(LivePayloads.decodeTelemetry(null))
        assertNull(LivePayloads.decodeCommand(null))
        assertNull(LivePayloads.decodePeerStatus(null))
    }
}
