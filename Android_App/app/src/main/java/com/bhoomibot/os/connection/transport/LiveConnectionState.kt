// ============================================================================
// LiveConnectionState.kt
// ----------------------------------------------------------------------------
// The single source of truth for "where is the live-link socket right now?".
// Both the low-level client (WebSocketLiveLinkClient) and the repository above
// it expose this as a StateFlow so the UI can show a connection indicator.
//
// Normal happy path:      IDLE -> CONNECTING -> CONNECTED
// Drop with reconnect on: CONNECTED -> RECONNECTING -> (CONNECTING) -> CONNECTED
// Drop with reconnect off: ... -> ERROR (terminal; the client stops trying)
// ============================================================================
package com.bhoomibot.os.connection.transport

/** Lifecycle of a live-link socket. See the file header for the transition diagram. */
enum class LiveConnectionState {
    IDLE,        // not connected
    CONNECTING,  // opening the socket
    CONNECTED,   // socket open, HELLO accepted
    RECONNECTING,// dropped, waiting to retry (only when autoReconnect is on)
    ERROR        // terminal failure (e.g. reconnect disabled, or a bad server URL)
}
