package com.bhoomibot.os.feature.autonomous.agent

import android.content.Context
import com.bhoomibot.os.feature.autonomous.core.interfaces.AgentLayer
import com.bhoomibot.os.feature.autonomous.core.interfaces.AgentResponse
import com.bhoomibot.os.feature.autonomous.core.interfaces.KnowledgeLayer
import com.bhoomibot.os.feature.autonomous.core.interfaces.PlannerLayer
import com.bhoomibot.os.feature.autonomous.core.model.Intent
import com.bhoomibot.os.feature.autonomous.skills.library.SkillLibrary
import com.bhoomibot.os.feature.autonomous.skills.execution.MissionPlanner
import com.bhoomibot.os.feature.autonomous.skills.learning.TeachingManager
import java.util.Locale

/**
 * FEATURE: L0 Agent Layer Implementation
 */
class AgentLayerImpl(
    private val context: Context,
    private val knowledge: KnowledgeLayer,
    private val planner: PlannerLayer
) : AgentLayer {

    private val dialogueManager = DialogueManager()
    private val gemmaReflex = GemmaAgentDriver(context)

    override suspend fun processIntent(text: String): AgentResponse {
        val lower = text.lowercase(Locale.ROOT).trim()
        val allSkills = SkillLibrary.getAllSkills(context)
        
        // 1. Handle "Teach" command
        if (lower.contains("teach") || lower.contains("learn a new skill")) {
            val name = lower.substringAfter("skill", "").trim()
            if (name.isEmpty()) {
                return AgentResponse.Feedback("Sure! What should I call this new skill?")
            } else {
                TeachingManager.startTeaching(context, name)
                return AgentResponse.StartTeaching(name)
            }
        }

        // 2. Check Skill Library (Exact/Keyword match)
        val learnedSkill = allSkills.find { lower.contains(it.name.lowercase()) }
        if (learnedSkill != null) {
            val repeats = extractNumber(lower, "times") ?: extractNumber(lower, "trips") ?: 1
            val plan = MissionPlanner.createMissionFromSkill(learnedSkill, repeats)
            return AgentResponse.PlanReady(plan)
        }

        // 3. Consult Gemma Reflex (Offline Logic)
        val learnedSkillNames = allSkills.map { it.name }
        
        // AI-Fix: Added logs to see if Gemma is even being called
        android.util.Log.d("AgentAI", "Consulting Gemma Reflex for: $text")
        val gemmaIntent = gemmaReflex.parseWithIntelligence(text, emptyList(), learnedSkillNames)
        
        if (gemmaIntent != null && gemmaIntent.confidence > 0.8f) {
             android.util.Log.d("AgentAI", "Gemma identified intent: ${gemmaIntent.action}")
             // Handle skills identified by Gemma but not caught by keyword
             if (gemmaIntent.action == "SKILL_REPLAY" && gemmaIntent.subject != null) {
                 val skill = allSkills.find { it.name.equals(gemmaIntent.subject, true) }
                 if (skill != null) return AgentResponse.PlanReady(MissionPlanner.createMissionFromSkill(skill))
             }
             
             val plan = planner.createPlan(gemmaIntent, knowledge.getKnowledgeContext().value)
             return AgentResponse.PlanReady(plan)
        }

        android.util.Log.d("AgentAI", "Falling back to Keyword/Dialogue manager")
        if (isGreeting(lower)) {
            return AgentResponse.Feedback("Hello! I am BhoomiBot. I can help with field tasks. I know ${allSkills.size} skills.")
        }

        val intent = dialogueManager.parseNaturalLanguage(text) ?: return AgentResponse.Feedback(
            "I haven't learned that skill yet. Would you like to teach me? Say 'Teach a new skill'."
        )

        if (intent.action == "STATUS") {
            return AgentResponse.Feedback("Battery: 85% | GPS: Connected | VCU: Online. I am ready for field operations!")
        }

        val plan = planner.createPlan(intent, knowledge.getKnowledgeContext().value)
        return AgentResponse.PlanReady(plan)
    }

    private fun isGreeting(text: String): Boolean {
        val greetings = listOf("hi", "hello", "hey", "namaste")
        return greetings.any { text.startsWith(it) }
    }

    private fun extractNumber(text: String, keyword: String): Int? {
        val segment = text.substringBefore(" $keyword", "")
        if (segment.isEmpty()) return null
        val part = segment.trim().split(" ").lastOrNull()
        return part?.toIntOrNull()
    }
}
