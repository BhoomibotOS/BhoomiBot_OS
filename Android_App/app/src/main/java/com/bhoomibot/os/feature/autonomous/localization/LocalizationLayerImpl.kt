package com.bhoomibot.os.feature.autonomous.localization

import android.content.Context
import com.bhoomibot.os.data.LocationTracker
import com.bhoomibot.os.feature.autonomous.core.interfaces.LocalizationLayer
import com.bhoomibot.sdk.RobotPose
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * FEATURE: L5 Localization Layer Implementation
 */
class LocalizationLayerImpl(context: Context) : LocalizationLayer {

    private val locationTracker = LocationTracker(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private val _pose = MutableStateFlow(RobotPose(0.0, 0.0, 0.0, 0.0, 0f, 0L))
    
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
                        timestamp = loc.time
                    )
                }
            }
        }
    }

    override fun getRobotPose(): StateFlow<RobotPose> = _pose.asStateFlow()

    override suspend fun updateSensorData(gps: Any, imu: Any) {}
}
