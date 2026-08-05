package com.bhoomibot.os.data

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * High-level wrapper for Android's LocationManager.
 *
 * Provides real-time GPS coordinates for mission recording and map visualization.
 * Handles permission checks internally (returns null if not granted).
 */
class LocationTracker(private val context: Context) : LocationListener {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    
    private val _currentLocation = MutableStateFlow<Location?>(null)
    val currentLocation: StateFlow<Location?> = _currentLocation.asStateFlow()

    private var isTracking = false

    @SuppressLint("MissingPermission")
    fun startTracking() {
        if (isTracking) return
        
        try {
            // Request updates from both GPS and Network for best results
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    500L, // 500ms min interval
                    0.5f,  // 0.5m min distance
                    this
                )
                isTracking = true
            }
            
            // Initial last known location
            val lastGps = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val lastNetwork = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            _currentLocation.value = lastGps ?: lastNetwork
            
        } catch (e: SecurityException) {
            // Permission not granted
            _currentLocation.value = null
        }
    }

    fun stopTracking() {
        locationManager.removeUpdates(this)
        isTracking = false
    }

    override fun onLocationChanged(location: Location) {
        _currentLocation.value = location
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
}
