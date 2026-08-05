package com.bhoomibot.os.feature.autonomous.core.model

import android.graphics.RectF
import com.bhoomibot.os.model.Waypoint

/**
 * FEATURE: AgentOS Robotics Execution Models
 * 
 * JUNIOR ENGINEER NOTE: These models represent the physical reality of the robot.
 * They are used by layers L4 through L8 (Perception to Hardware).
 */

/**
 * RobotPose: The precise estimated state of the robot in the world.
 * Produced by L5: Localization Layer.
 */
data class RobotPose(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0,
    val heading: Double, // 0-360 degrees (North-based)
    val speedMps: Float, // Current velocity in Meters Per Second
    val accuracyMeters: Float,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Observation: Something detected by the robot's sensors (Camera/IMU).
 * Produced by L4: Perception Layer.
 */
data class Observation(
    val label: String, // "WEED", "CROP", "HUMAN", "ROCK"
    val confidence: Float, // 0.0 to 1.0
    val boundingBox: RectF, // Screen coordinates (0.0 to 1.0)
    val estimatedDistance: Float? = null, // Distance in meters if depth is available
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Trajectory: A mathematical path for the robot to follow.
 * Produced by L3: Skill Registry or L6: World Model.
 */
data class Trajectory(
    val waypoints: List<Waypoint>,
    val targetSpeedMps: Float,
    val pathType: PathType = PathType.FIXED_LINE
)

/**
 * VelocityCommand: The raw motion instructions for the drive system.
 * Produced by L7: Motion Control.
 */
data class VelocityCommand(
    val linearVelocity: Float, // -1.0 (Reverse) to 1.0 (Forward)
    val angularVelocity: Float, // -1.0 (Left) to 1.0 (Right)
    val timestamp: Long = System.currentTimeMillis()
)

enum class PathType { FIXED_LINE, CURVE, DYNAMIC_AVOIDANCE }
