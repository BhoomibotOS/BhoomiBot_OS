package com.bhoomibot.os.feature.autonomous.simulation.hardware

import com.bhoomibot.os.feature.autonomous.simulation.engine.DigitalTwin
import com.bhoomibot.os.model.Waypoint
import kotlinx.coroutines.flow.map

/**
 * MOCK GPS: Translates Digital Twin coordinates into GPS Waypoints.
 * Adds synthetic "Gaussian Noise" to simulate real-world inaccuracy.
 */
class MockGps {
    
    // Constant for UTM to GPS conversion (Conceptual)
    private val LAT_ORIGIN = 12.9716
    private val LON_ORIGIN = 77.5946

    val locationStream = DigitalTwin.state.map { twin ->
        // Add random 0.5m drift to simulate standard GPS
        val driftLat = (Math.random() - 0.5) * 0.000005
        val driftLon = (Math.random() - 0.5) * 0.000005
        
        Waypoint(
            latitude = LAT_ORIGIN + (twin.y * 0.00001) + driftLat,
            longitude = LON_ORIGIN + (twin.x * 0.00001) + driftLon,
            timestamp = twin.timestamp,
            accuracy = 1.2f
        )
    }
}
