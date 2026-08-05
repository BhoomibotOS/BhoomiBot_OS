package com.bhoomibot.os.model

import kotlinx.serialization.Serializable

/**
 * Single recorded command with full robot state snapshot.
 */
@Serializable
data class CommandRecord(
    val drive: DriveCommand = DriveCommand.STOP,
    val speedPercent: Int = 0,
    val ptoEnabled: Boolean = false,
    val ptoSpeedPercent: Int = 0,
    val hydraulicEnabled: Boolean = false,
    val hydraulicHeightPercent: Int = 0,
    val lightsEnabled: Boolean = false,
    val cameraLightEnabled: Boolean = false,
    val hornTriggered: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val heading: Double = 0.0,
    val gpsAccuracy: Float = 0.0f,
    val marker: String? = null
)
