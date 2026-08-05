package com.bhoomibot.os.feature.autonomous

import android.content.Context
import com.bhoomibot.os.connection.repository.LiveLinkRepository
import com.bhoomibot.os.data.LocationTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.*

/**
 * Monitors the safety of autonomous operations.
 */
class SafetyMonitor(
    private val context: Context,
    private val stateMachine: AutonomyStateMachine,
    private val liveLinkRepository: LiveLinkRepository
) {
    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private val locationTracker = LocationTracker(context)
    private var monitoringJob: Job? = null
    
    private var lastLat: Double? = null
    private var lastLon: Double? = null
    private var stallCounter = 0

    fun startMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = scope.launch {
            stateMachine.state.collectLatest { state ->
                if (state == AutonomyState.EXECUTING) {
                    runSafetyChecks()
                }
            }
        }
    }

    private suspend fun runSafetyChecks() {
        locationTracker.startTracking()
        while (stateMachine.state.value == AutonomyState.EXECUTING) {
            val loc = locationTracker.currentLocation.value
            
            if (loc != null) {
                // 1. Check for Stall (AI-003)
                if (lastLat != null && lastLon != null) {
                    val dist = calculateDistance(lastLat!!, lastLon!!, loc.latitude, loc.longitude)
                    if (dist < 0.2) { // Moved less than 20cm in 1 second
                        stallCounter++
                    } else {
                        stallCounter = 0
                    }
                }
                
                if (stallCounter >= 5) { // Stalled for 5 seconds
                    stateMachine.transitionTo(AutonomyState.ERROR)
                    liveLinkRepository.publishAlert(
                        message = "ROBOT STALLED: Position not changing despite drive command. Possible obstacle or mechanical failure.",
                        severity = "HIGH"
                    )
                    stallCounter = 0
                }
                
                lastLat = loc.latitude
                lastLon = loc.longitude
            }

            delay(1000) // Check once per second
        }
        locationTracker.stopTracking()
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371e3
        val phi1 = lat1 * PI / 180
        val phi2 = lat2 * PI / 180
        val deltaPhi = (lat2 - lat1) * PI / 180
        val deltaLambda = (lon2 - lon1) * PI / 180
        val a = sin(deltaPhi / 2).pow(2) + cos(phi1) * cos(phi2) * sin(deltaLambda / 2).pow(2)
        return r * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    fun stopMonitoring() {
        monitoringJob?.cancel()
    }
}
