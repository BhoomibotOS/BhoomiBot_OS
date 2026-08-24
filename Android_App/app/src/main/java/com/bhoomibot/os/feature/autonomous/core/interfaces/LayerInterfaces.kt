package com.bhoomibot.os.feature.autonomous.core.interfaces

import android.graphics.Bitmap
import com.bhoomibot.sdk.RobotIntent
import com.bhoomibot.sdk.KnowledgeNode
import com.bhoomibot.sdk.TaskPlan
import com.bhoomibot.sdk.SkillStep
import com.bhoomibot.sdk.Skill
import com.bhoomibot.sdk.Trajectory
import com.bhoomibot.sdk.Observation
import com.bhoomibot.sdk.RobotPose
import com.bhoomibot.sdk.VelocityCommand
import kotlinx.coroutines.flow.StateFlow

/**
 * FEATURE: AgentOS Standardized Layer Interfaces
 */

// L0: Agent Layer
interface AgentLayer {
    suspend fun processIntent(text: String): AgentResponse
}

sealed class AgentResponse {
    data class PlanReady(val plan: com.bhoomibot.sdk.TaskPlan) : AgentResponse()
    data class Feedback(val message: String) : AgentResponse()
    data class StartTeaching(val skillName: String) : AgentResponse()
    data class ClarificationNeeded(val query: String, val options: List<String>) : AgentResponse()
}

// L1: Knowledge Layer
interface KnowledgeLayer {
    suspend fun queryNode(name: String): com.bhoomibot.sdk.KnowledgeNode?
    suspend fun storeNode(node: com.bhoomibot.sdk.KnowledgeNode)
    fun getKnowledgeContext(): StateFlow<List<com.bhoomibot.sdk.KnowledgeNode>>
}

// L2: Task Planner
interface PlannerLayer {
    suspend fun createPlan(intent: com.bhoomibot.sdk.RobotIntent, context: List<com.bhoomibot.sdk.KnowledgeNode>): com.bhoomibot.sdk.TaskPlan
}

// L3: Skill Layer
interface SkillLayer {
    suspend fun executeSkill(skillExecution: com.bhoomibot.sdk.SkillStep): com.bhoomibot.sdk.Trajectory
    fun getAvailableSkills(): List<com.bhoomibot.sdk.Skill>
}

// L4: Perception Layer
interface PerceptionLayer {
    suspend fun analyzeFrame(bitmap: Bitmap): List<com.bhoomibot.sdk.Observation>
}

// L5: Localization Layer
interface LocalizationLayer {
    fun getRobotPose(): StateFlow<com.bhoomibot.sdk.RobotPose>
    suspend fun updateSensorData(gps: Any, imu: Any)
}

// L6: World Model Layer
interface WorldModelLayer {
    fun getSemanticMap(): StateFlow<List<com.bhoomibot.sdk.KnowledgeNode>>
    suspend fun syncPerception(pose: com.bhoomibot.sdk.RobotPose, observations: List<com.bhoomibot.sdk.Observation>)
}

// L7: Control Layer
interface ControlLayer {
    fun calculateMotion(trajectory: com.bhoomibot.sdk.Trajectory, currentPose: com.bhoomibot.sdk.RobotPose): com.bhoomibot.sdk.VelocityCommand
}

// L8: Hardware Layer
interface HardwareLayer {
    suspend fun sendMotion(command: com.bhoomibot.sdk.VelocityCommand)
    suspend fun triggerAuxiliary(toolId: String, action: String)
}
