package com.bhoomibot.os.feature.autonomous.localization

import android.content.Context
import com.bhoomibot.os.data.LocationTracker
import com.bhoomibot.os.feature.autonomous.core.interfaces.LocalizationLayer
import com.bhoomibot.os.feature.autonomous.core.model.RobotPose
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * FEATURE: L5 Localization Layer Implementation
 * 
 * JUNIOR ENGINEER NOTE: This layer uses the phone's GPS to tell the robot "Where am I".
 * It listens to the LocationTracker and converts it into a standard RobotPose.
 */
class LocalizationLayerImpl(context: Context) : LocalizationLayer {

    private val locationTracker = LocationTracker(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private val _pose = MutableStateFlow(RobotPose(0.0, 0.0, 0.0, 0.0, 0f, 0f))
    
    init {
        locationTracker.startTracking()
        scope.launch {
            locationTracker.currentLocation.collect { loc ->
                if (loc != null) {
                    _pose.value = RobotPose(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        altitude = loc.altitude,
                        heading = loc.bearing.toDouble(),
                        speedMps = loc.speed,
                        accuracyMeters = loc.accuracy,
                        timestamp = loc.time
                    )
                }
            }
        }
    }

    override fun getRobotPose(): StateFlow<RobotPose> = _pose.asStateFlow()

    override suspend fun updateSensorData(gps: Any, imu: Any) {
        // Future: Handle raw NMEA or IMU frames for higher frequency EKF fusion
    }
}
