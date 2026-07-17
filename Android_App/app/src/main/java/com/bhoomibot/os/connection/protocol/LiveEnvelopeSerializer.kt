package com.bhoomibot.os.connection.protocol

import com.bhoomibot.os.connection.model.LiveEnvelope
import org.json.JSONObject

/**
 * JSON (de)serializer for the live-link envelope. Uses the built-in [org.json]
 * (no external dependency) so it stays light and unit-testable on the JVM.
 *
 * Wire shape (per Communication Master):
 *   { type, robotId, ts, payload, ack, code, retry }
 */
object LiveEnvelopeSerializer {
    fun encode(e: LiveEnvelope): String = JSONObject().apply {
        put("type", e.type)
        put("robotId", e.robotId)
        put("ts", e.ts)
        putOpt("payload", e.payload) // putOpt omits the key entirely when payload is null
        put("ack", e.ack)
        put("code", e.code)
        put("retry", e.retry)
    }.toString()

    // Returns null on ANY parse problem (malformed JSON, missing required "type")
    // rather than throwing — the socket callback treats null as "drop this message".
    fun decode(json: String): LiveEnvelope? = runCatching {
        val o = JSONObject(json)
        LiveEnvelope(
            type = o.getString("type"), // required; getString throws -> runCatching -> null
            // optX(key, default) = "use default if the key is absent". The payload
            // needs the extra isNull check to tell JSON null apart from a real string.
            robotId = o.optString("robotId", ""),
            ts = o.optLong("ts", 0L),
            payload = if (o.isNull("payload")) null else o.optString("payload"),
            ack = o.optBoolean("ack", false),
            code = o.optInt("code", 0),
            retry = o.optInt("retry", 0)
        )
    }.getOrNull()
}
