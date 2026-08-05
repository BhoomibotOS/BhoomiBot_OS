package com.bhoomibot.os.connection.model

/** Message kinds on the live link, matching the Communication Master envelope. */
enum class LiveMessageType(val code: String) {
    HELLO("HELLO"),
    VIDEO_FRAME("VIDEO_FRAME"),
    TELEMETRY("TELEMETRY"),
    COMMAND("COMMAND"),
    ACK("ACK"),
    ERROR("ERROR"),
    PEER_STATUS("PEER_STATUS"),
    PING("PING"),
    PONG("PONG"),
    ALERT("ALERT");

    companion object {
        // Parse a wire `type` string back into an enum. Unknown/absent codes map to
        // ERROR rather than throwing, so a malformed or future message type can't
        // crash the receive loop (LiveLinkRepositoryImpl just ignores it via `else`).
        fun from(code: String?): LiveMessageType =
            entries.firstOrNull { it.code == code } ?: ERROR
    }
}
