package com.bhoomibot.os.feature.autonomous

import com.bhoomibot.os.model.Waypoint
import kotlin.math.*

/**
 * Coordinates following a list of waypoints using Haversine formula for distance.
 * 
 * Provides logic to detect when a waypoint is reached and to calculate the
 * required speed adjustment based on distance to the target.
 */
class WaypointTracker(private val waypoints: List<Waypoint>) {

    private var currentWaypointIndex = 0
    private val arrivalThresholdMeters = 5.0
    private val decelerationDistanceMeters = 50.0

    /**
     * Returns the current target waypoint or null if all reached.
     */
    fun getTargetWaypoint(): Waypoint? {
        return if (currentWaypointIndex < waypoints.size) waypoints[currentWaypointIndex] else null
    }

    /**
     * Checks if current position is within arrival threshold of the target waypoint.
     * If so, advances to the next waypoint.
     */
    fun updateProgress(currentLat: Double, currentLng: Double): Boolean {
        val target = getTargetWaypoint() ?: return true
        
        val distance = calculateDistance(currentLat, currentLng, target.latitude, target.longitude)
        
        if (distance < arrivalThresholdMeters) {
            currentWaypointIndex++
            return true // Waypoint reached
        }
        return false
    }

    /**
     * Calculates speed multiplier (0.0 to 1.0) based on distance to target.
     * Slows down as it approaches the waypoint.
     */
    fun getSpeedMultiplier(currentLat: Double, currentLng: Double): Float {
        val target = getTargetWaypoint() ?: return 0f
        val distance = calculateDistance(currentLat, currentLng, target.latitude, target.longitude)
        
        return when {
            distance > decelerationDistanceMeters -> 1.0f
            distance < arrivalThresholdMeters -> 0.0f
            else -> {
                // Proportional slowdown between 50m and 5m
                ((distance - arrivalThresholdMeters) / (decelerationDistanceMeters - arrivalThresholdMeters)).toFloat()
            }
        }.coerceIn(0f, 1f)
    }

    /**
     * Returns completion percentage (0-100).
     */
    fun getProgressPercent(): Int {
        if (waypoints.isEmpty()) return 100
        return (currentWaypointIndex * 100 / waypoints.size).coerceIn(0, 100)
    }

    /**
     * Haversine formula to calculate distance between two coordinates in meters.
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371e3 // Earth radius in meters
        val phi1 = lat1 * PI / 180
        val phi2 = lat2 * PI / 180
        val deltaPhi = (lat2 - lat1) * PI / 180
        val deltaLambda = (lon2 - lon1) * PI / 180

        val a = sin(deltaPhi / 2).pow(2) +
                cos(phi1) * cos(phi2) *
                sin(deltaLambda / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return r * c
    }
}
