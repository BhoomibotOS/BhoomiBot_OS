package com.bhoomibot.os.feature.autonomous.skills.models

import kotlinx.serialization.Serializable

@Serializable
enum class ActionType { NAVIGATE, ATTACH, DETACH, WAIT, ACTUATE_PTO }

@Serializable
data class SkillStep(
    val sequence: Int,
    val actionType: ActionType,
    val targetKnowledgeNodeId: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val parameters: Map<String, String> = emptyMap(),
    val stopOnArrival: Boolean = true
)

@Serializable
data class DemonstratedSkill(
    val id: String,
    val name: String,
    val steps: List<SkillStep>,
    val createdTimestamp: Long = System.currentTimeMillis()
)
