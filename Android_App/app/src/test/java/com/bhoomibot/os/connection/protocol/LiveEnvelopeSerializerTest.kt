// ============================================================================
// LiveEnvelopeSerializerTest.kt
// ----------------------------------------------------------------------------
// Pure-JVM unit tests for the wire-envelope JSON codec. These run without an
// emulator precisely because LiveEnvelopeSerializer uses org.json (not Android's
// JSON) and has no Android dependencies. Coverage: full round-trip fidelity,
// defaulting of absent optional fields, and null on malformed input.
// ============================================================================
package com.bhoomibot.os.connection.protocol

import com.bhoomibot.os.connection.model.LiveEnvelope
import com.bhoomibot.os.connection.model.LiveMessageType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LiveEnvelopeSerializerTest {

    @Test
    fun `encode then decode round-trips`() {
        val payload = "{\"drive\":\"FORWARD\"}"
        val e = LiveEnvelope(
            type = LiveMessageType.COMMAND.code,
            robotId = "R1",
            ts = 123L,
            payload = payload,
            ack = true,
            code = 0,
            retry = 2
        )
        val back = LiveEnvelopeSerializer.decode(LiveEnvelopeSerializer.encode(e))!!
        assertEquals(e.type, back.type)
        assertEquals("R1", back.robotId)
        assertEquals(123L, back.ts)
        assertEquals(payload, back.payload)
        assertEquals(true, back.ack)
        assertEquals(2, back.retry)
    }

    @Test
    fun `missing optional fields default`() {
        val json = "{\"type\":\"HELLO\",\"robotId\":\"R9\"}"
        val e = LiveEnvelopeSerializer.decode(json)!!
        assertEquals(LiveMessageType.HELLO.code, e.type)
        assertEquals("R9", e.robotId)
        assertEquals(0L, e.ts)
        assertNull(e.payload)
        assertEquals(false, e.ack)
    }

    @Test
    fun `invalid json returns null`() {
        assertNull(LiveEnvelopeSerializer.decode("not json"))
        assertNull(LiveEnvelopeSerializer.decode(""))
    }
}
