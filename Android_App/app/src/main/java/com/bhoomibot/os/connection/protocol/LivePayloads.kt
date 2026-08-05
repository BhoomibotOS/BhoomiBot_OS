package com.bhoomibot.os.connection.protocol

import com.bhoomibot.os.connection.model.PeerStatus
import com.bhoomibot.os.connection.model.RobotCommand
import com.bhoomibot.os.connection.model.TelemetrySnapshot
import com.bhoomibot.os.model.DriveCommand
import org.json.JSONObject

/**
 * Codecs for the JSON payloads carried inside the live-link envelope:
 * telemetry (ROBOT -> OPERATOR), commands (OPERATOR -> ROBOT), and
 * peer-status (relayed by the server). Built on [org.json] for zero extra deps.
 */
object LivePayloads {
    fun encodeTelemetry(t: TelemetrySnapshot): String = JSONObject().apply {
        put("isOnline", t.isOnline)
        put("batteryPercent", t.batteryPercent)
        put("mode", t.mode)
        put("mission", t.mission)
        put("gpsStatus", t.gpsStatus)
        put("cameraStatus", t.cameraStatus)
        put("aiStatus", t.aiStatus)
    }.toString()

    fun decodeTelemetry(json: String?): TelemetrySnapshot? = runCatching {
        val o = JSONObject(json ?: return@runCatching null)
        TelemetrySnapshot(
            isOnline = o.optBoolean("isOnline", false),
            batteryPercent = o.optInt("batteryPercent", 0),
            mode = o.optString("mode", ""),
            mission = o.optString("mission", ""),
            gpsStatus = o.optString("gpsStatus", ""),
            cameraStatus = o.optString("cameraStatus", ""),
            aiStatus = o.optString("aiStatus", "")
        )
    }.getOrNull()

    fun encodeCommand(c: RobotCommand): String = JSONObject().apply {
        put("drive", c.drive.name)
        put("speedPercent", c.speedPercent)
        put("emergencyStop", c.emergencyStop)
        putOpt("pto", c.pto)
        putOpt("lights", c.lights)
        putOpt("liveCamera", c.liveCamera)
        putOpt("useRearCamera", c.useRearCamera)
    }.toString()

    fun decodeCommand(json: String?): RobotCommand? = runCatching {
        val o = JSONObject(json ?: return@runCatching null)
        RobotCommand(
            // Nested runCatching: an unknown drive string (valueOf throws) falls back
            // to STOP — the safe default — instead of failing the whole decode.
            drive = runCatching { DriveCommand.valueOf(o.optString("drive", "STOP")) }
                .getOrDefault(DriveCommand.STOP),
            speedPercent = o.optInt("speedPercent", 0),
            emergencyStop = o.optBoolean("emergencyStop", false),
            // isNull preserves the tri-state: JSON null -> null ("leave unchanged").
            pto = if (o.isNull("pto")) null else o.optBoolean("pto"),
            lights = if (o.isNull("lights")) null else o.optBoolean("lights"),
            liveCamera = if (o.isNull("liveCamera")) null else o.optBoolean("liveCamera"),
            useRearCamera = if (o.isNull("useRearCamera")) null else o.optBoolean("useRearCamera")
        )
    }.getOrNull()

    fun encodePeerStatus(p: PeerStatus): String = JSONObject().apply {
        put("robot", p.robotOnline)
        put("operator", p.operatorOnline)
    }.toString()

    fun decodePeerStatus(json: String?): PeerStatus? = runCatching {
        val o = JSONObject(json ?: return@runCatching null)
        PeerStatus(
            robotOnline = o.optBoolean("robot", false),
            operatorOnline = o.optBoolean("operator", false)
        )
    }.getOrNull()

    fun encodeAlert(message: String, severity: String): String = JSONObject().apply {
        put("message", message)
        put("severity", severity)
        put("timestamp", System.currentTimeMillis())
    }.toString()

    fun decodeAlert(json: String?): Pair<String, String>? = runCatching {
        val o = JSONObject(json ?: return@runCatching null)
        o.optString("message", "Unknown problem") to o.optString("severity", "LOW")
    }.getOrNull()
}
