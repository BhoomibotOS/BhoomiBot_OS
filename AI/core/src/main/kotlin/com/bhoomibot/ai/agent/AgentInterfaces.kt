package com.bhoomibot.ai.agent

import com.bhoomibot.sdk.RobotIntent

/** Interface for LLM-based intent parsing */
interface AgentDriver {
    suspend fun parseIntent(text: String, learnedSkills: List<String>): RobotIntent?
}

/** High-level Agent Bridge */
interface BrainAgent {
    suspend fun processCommand(text: String): BrainResponse
}

sealed class BrainResponse {
    data class Plan(val steps: List<com.bhoomibot.sdk.SkillStep>) : BrainResponse()
    data class Question(val query: String) : BrainResponse()
    data class Feedback(val message: String) : BrainResponse()
}
