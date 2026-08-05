package com.bhoomibot.os.model

import kotlinx.serialization.Serializable

/**
 * GPS waypoint used for recorded paths.
 *
 * Represents a geographic coordinate with precision metadata.
 * Used exclusively for mission recording and playback navigation.
 *
 * @property latitude Geographic latitude in decimal degrees
 * @property longitude Geographic longitude in decimal degrees
 * @property timestamp Unix time in milliseconds when location was recorded
 * @property accuracy Estimated GPS accuracy in meters
 */
@Serializable
data class Waypoint(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long,
    val accuracy: Float
)