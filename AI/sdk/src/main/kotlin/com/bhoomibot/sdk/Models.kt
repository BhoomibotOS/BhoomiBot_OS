package com.bhoomibot.sdk

import kotlinx.serialization.Serializable

/** Standardized Robot Pose (Platform Independent) */
@Serializable
data class RobotPose(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0,
    val heading: Double, // 0-360 degrees
    val speedMps: Float,
    val timestamp: Long
)

/** Portable Vision Bounding Box */
@Serializable
data class BoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

/** Standardized Vision Observation */
@Serializable
data class Observation(
    val label: String,
    val confidence: Float,
    val box: BoundingBox,
    val timestamp: Long
)

/** The High-Level Goal */
@Serializable
data class RobotIntent(
    val action: RobotAction,
    val subject: String? = null,
    val source: String? = null,
    val destination: String? = null,
    val repeatCount: Int = 1,
    val rawText: String? = null
)

enum class RobotAction {
    NAVIGATE, TRANSPORT, SPRAY, HARVEST, STATUS, STOP, LEARN, SKILL_REPLAY, HELP
}

/** A physical move definition */
@Serializable
data class Skill(
    val id: String,
    val name: String
)

/** A logical step in a plan */
@Serializable
data class SkillStep(
    val skillId: String,
    val parameters: Map<String, String> = emptyMap()
)

/** A multi-step mission plan */
@Serializable
data class TaskPlan(
    val steps: List<SkillStep>
)

/** Global GPS Waypoint for AI Core */
@Serializable
data class BrainWaypoint(
    val latitude: Double,
    val longitude: Double,
    val timestamp: Long = 0L,
    val accuracy: Float = 0f
)

/** A mathematical path */
@Serializable
data class Trajectory(
    val waypoints: List<BrainWaypoint> = emptyList(),
    val targetSpeedMps: Float = 0f
)

/** Raw motion command */
@Serializable
data class VelocityCommand(
    val linearVelocity: Float,
    val angularVelocity: Float,
    val timestamp: Long = System.currentTimeMillis()
)

/** A logical entity in the world model */
@Serializable
data class KnowledgeNode(
    val name: String,
    val type: NodeType,
    val metadata: Map<String, String> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
)

enum class NodeType {
    POINT, AREA, OBJECT, SKILL, DYNAMIC, OBSTACLE
}
