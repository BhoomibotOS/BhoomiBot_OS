package com.bhoomibot.os.connection.model

import com.bhoomibot.os.model.RobotStatus

// Bridges the app's internal domain model (RobotStatus, produced by the robot's
// on-board logic) to the wire model (TelemetrySnapshot, what actually gets sent).
// Keeping them separate means changing an internal field never accidentally
// alters the network contract. The ROBOT side calls this before publishTelemetry.

/** Maps the app's existing [RobotStatus] into the wire [TelemetrySnapshot]. */
fun RobotStatus.toTelemetry(): TelemetrySnapshot = TelemetrySnapshot(
    isOnline = isOnline,
    batteryPercent = batteryPercent,
    mode = mode,
    mission = mission,
    gpsStatus = gpsStatus,
    cameraStatus = cameraStatus,
    aiStatus = aiStatus
)
