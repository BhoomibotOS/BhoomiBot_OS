// ============================================================================
// ConnectionOptionsUiStateTest.kt
// ----------------------------------------------------------------------------
// Protects the setup screen's safety rule: public relay connections use `wss`,
// while plaintext `ws` is allowed only for a deliberate local Wi-Fi relay.
// ============================================================================
package com.bhoomibot.os.feature.connection

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionOptionsUiStateTest {

    private fun state(url: String, mode: PhoneNetworkMode) = ConnectionOptionsUiState(
        serverUrl = url,
        robotId = "robot-1",
        sessionCode = "session-123",
        networkMode = mode
    )

    @Test
    fun `internet mode accepts a complete Render wss URL`() {
        assertTrue(state("wss://bhoomibot-os.onrender.com", PhoneNetworkMode.INTERNET).canStart)
    }

    @Test
    fun `internet mode rejects insecure ws URL`() {
        assertFalse(state("ws://bhoomibot-os.onrender.com", PhoneNetworkMode.INTERNET).canStart)
    }

    @Test
    fun `internet mode rejects a scheme without a host`() {
        assertFalse(state("wss://", PhoneNetworkMode.INTERNET).canStart)
    }

    @Test
    fun `same Wi-Fi mode accepts a complete local ws URL`() {
        assertTrue(state("ws://192.168.1.10:8080", PhoneNetworkMode.LOCAL_WIFI).canStart)
    }
}
