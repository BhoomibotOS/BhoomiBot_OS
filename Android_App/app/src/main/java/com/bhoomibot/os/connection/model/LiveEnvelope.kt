package com.bhoomibot.os.connection.model

/**
 * The wire envelope used for every JSON message on the live link.
 * Shape is fixed by the Communication Master:
 *   { type, robotId, ts, payload, ack, code, retry }
 *
 * `payload` carries a JSON string for control/telemetry/peer-status, and is null
 * for binary video frames (those travel as raw bytes, not inside this envelope).
 */
data class LiveEnvelope(
    val type: String,          // LiveMessageType code, e.g. "TELEMETRY" / "COMMAND"
    val robotId: String,       // which robot/session this message belongs to
    val ts: Long = 0L,         // sender's epoch-millis timestamp
    val payload: String? = null, // nested JSON string (see LivePayloads); null for none
    // ack/code/retry are part of the fixed Communication Master spec. They are
    // carried through faithfully even though this app currently only reads/writes
    // a subset of them — keep them so the wire format stays compatible.
    val ack: Boolean = false,  // true if this message acknowledges a prior one
    val code: Int = 0,         // status/error code (0 = none)
    val retry: Int = 0         // sender's retry counter for this message
)
