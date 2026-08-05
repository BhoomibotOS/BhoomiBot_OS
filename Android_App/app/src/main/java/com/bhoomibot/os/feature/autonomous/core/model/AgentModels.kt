package com.bhoomibot.os.feature.autonomous.core.model

import java.util.UUID

/**
 * FEATURE: AgentOS High-Level Cognitive Models
 * 
 * JUNIOR ENGINEER NOTE: These models represent how the robot "thinks". 
 * They are decoupled from hardware and only deal with abstract goals and knowledge.
 */

/**
 * Intent: The extracted meaning of a user's natural language command.
 * Produced by L0: Agent Layer.
 */
data class Intent(
    val id: String = UUID.randomUUID().toString(),
    val action: String, // e.g., "TRANSPORT", "SPRAY", "LEARN"
    val subject: String?, // e.g., "FERTILIZER"
    val source: String? = null, // e.g., "SHED"
    val destination: String?, // e.g., "TOMATO_FIELD"
    val repeatCount: Int = 1, // e.g., 10
    val rawText: String,
    val confidence: Float
)

/**
 * Skill: A reusable atomic capability that the robot knows how to perform.
 * Managed by L3: Skill Registry.
 */
data class Skill(
    val id: String,
    val name: String, // e.g., "Navigate", "AttachTool"
    val requiredParameters: List<String> = emptyList()
)

/**
 * SkillExecution: A single step in a larger task plan.
 */
data class SkillExecution(
    val skillId: String,
    val parameters: Map<String, String> = emptyMap(),
    val status: ExecutionStatus = ExecutionStatus.QUEUED
)

/**
 * KnowledgeNode: A semantic entity in the Knowledge Graph (L1).
 * Can be a Zone, a Point, a Tool, or a Rule.
 */
data class KnowledgeNode(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: NodeType,
    val metadata: Map<String, String> = emptyMap(),
    val relationships: List<String> = emptyList() // List of linked Node IDs
)

/**
 * TaskPlan: A sequence of skill executions composed by L2: Task Planner.
 */
data class TaskPlan(
    val id: String = UUID.randomUUID().toString(),
    val originalIntentId: String,
    val steps: List<SkillExecution>,
    val currentStepIndex: Int = 0
)

enum class NodeType { ZONE, POINT, TOOL, RULE, OBSTACLE }
enum class ExecutionStatus { QUEUED, RUNNING, COMPLETED, FAILED }

/**
 * DialogueState: Tracks the multi-turn conversation context.
 */
data class DialogueState(
    val lastIntent: Intent? = null,
    val pendingQuestion: String? = null,
    val contextVariables: Map<String, String> = emptyMap(),
    val history: List<String> = emptyList()
)
