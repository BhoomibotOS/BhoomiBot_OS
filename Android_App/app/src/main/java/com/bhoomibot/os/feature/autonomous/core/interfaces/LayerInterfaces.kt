package com.bhoomibot.os.feature.autonomous.core.interfaces

import android.graphics.Bitmap
import com.bhoomibot.os.feature.autonomous.core.model.*
import kotlinx.coroutines.flow.StateFlow

/**
 * FEATURE: AgentOS Standardized Layer Interfaces
 */

// L0: Agent Layer - The ChatGPT for Robots
interface AgentLayer {
    suspend fun processIntent(text: String): AgentResponse
}

sealed class AgentResponse {
    data class PlanReady(val plan: TaskPlan) : AgentResponse()
    data class ClarificationNeeded(val query: String, val options: List<String>) : AgentResponse()
    data class Feedback(val message: String) : AgentResponse()
    data class StartTeaching(val skillName: String) : AgentResponse()
}

// L1: Knowledge Layer - The Semantic Memory
interface KnowledgeLayer {
    suspend fun queryNode(name: String): KnowledgeNode?
    suspend fun storeNode(node: KnowledgeNode)
    fun getKnowledgeContext(): StateFlow<List<KnowledgeNode>>
}

// L2: Task Planner - Strategic Logic
interface PlannerLayer {
    suspend fun createPlan(intent: Intent, context: List<KnowledgeNode>): TaskPlan
}

// L3: Skill Layer - Reusable Capabilities
interface SkillLayer {
    suspend fun executeSkill(skillExecution: SkillExecution): Trajectory
    fun getAvailableSkills(): List<Skill>
}

// L4: Perception Layer - Vision Engine
interface PerceptionLayer {
    suspend fun analyzeFrame(bitmap: Bitmap): List<Observation>
}

// L5: Localization Layer - Pose Estimation
interface LocalizationLayer {
    fun getRobotPose(): StateFlow<RobotPose>
    suspend fun updateSensorData(gps: Any, imu: Any)
}

// L6: World Model Layer - Digital Twin
interface WorldModelLayer {
    fun getSemanticMap(): StateFlow<List<KnowledgeNode>>
    suspend fun syncPerception(pose: RobotPose, observations: List<Observation>)
}

// L7: Control Layer - Physics & Steering
interface ControlLayer {
    fun calculateMotion(trajectory: Trajectory, currentPose: RobotPose): VelocityCommand
}

// L8: Hardware Layer - VCU Bridge
interface HardwareLayer {
    suspend fun sendMotion(command: VelocityCommand)
    suspend fun triggerAuxiliary(toolId: String, action: String)
}
