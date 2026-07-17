package com.bhoomibot.os.connection.model

// The wire representation of robot status (a plain, serializable snapshot).
// Fields are kept as simple Strings/primitives so the JSON codec in LivePayloads
// stays trivial; richer domain types live in the app model and are mapped in via
// TelemetryMappers.toTelemetry().

/** Robot health/state pushed ROBOT -> OPERATOR alongside the video feed. */
data class TelemetrySnapshot(
    val isOnline: Boolean = false,
    val batteryPercent: Int = 0,
    val mode: String = "",
    val mission: String = "",
    val gpsStatus: String = "",
    val cameraStatus: String = "",
    val aiStatus: String = ""
)
