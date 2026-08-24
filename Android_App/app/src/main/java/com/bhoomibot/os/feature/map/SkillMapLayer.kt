package com.bhoomibot.os.feature.map

import android.graphics.Color
import com.bhoomibot.sdk.RobotPose

/**
 * SkillMapLayer: Visualizes the robot's pose and path on Google Maps.
 */
class SkillMapLayer {

    fun createRobotMarker(pose: RobotPose): com.google.android.gms.maps.model.MarkerOptions {
        return com.google.android.gms.maps.model.MarkerOptions()
            .position(com.google.android.gms.maps.model.LatLng(pose.latitude, pose.longitude))
            .title("Robot Pose")
            .rotation(pose.heading.toFloat())
            .flat(true)
            .anchor(0.5f, 0.5f)
    }

    fun createPathPolyline(waypoints: List<com.bhoomibot.os.model.Waypoint>): com.google.android.gms.maps.model.PolylineOptions {
        val options = com.google.android.gms.maps.model.PolylineOptions()
            .width(5f)
            .color(Color.CYAN)
            .geodesic(true)
        
        waypoints.forEach { 
            options.add(com.google.android.gms.maps.model.LatLng(it.latitude, it.longitude))
        }
        return options
    }
}
