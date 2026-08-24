package com.bhoomibot.ai.agent

import com.bhoomibot.sdk.RobotAction
import com.bhoomibot.sdk.RobotIntent
import java.util.*

/**
 * L0 Rule-based Fallback Parser
 */
class DialogueManager {

    fun parse(text: String): RobotIntent? {
        val lower = text.lowercase(Locale.ROOT).trim()

        return when {
            containsAny(lower, listOf("shift", "take", "put", "place", "move", "carry", "load", "transport", "bring", "fetch", "deliver")) -> {
                val to = extractValue(lower, "to")
                val from = extractValue(lower, "from")
                // Extract subject: words between "carry/take" and "from/to"
                val subject = extractSubject(lower, listOf("shift", "take", "put", "place", "move", "carry", "load", "transport", "bring", "fetch", "deliver"), listOf("from", "to"))
                val count = extractNumber(lower) ?: 1
                RobotIntent(
                    action = RobotAction.TRANSPORT, 
                    subject = subject, 
                    source = from, 
                    destination = to, 
                    repeatCount = count,
                    rawText = text
                )
            }
            containsAny(lower, listOf("go to", "drive to", "reach")) -> {
                val dest = extractValue(lower, "to")
                RobotIntent(action = RobotAction.NAVIGATE, destination = dest, rawText = text)
            }
            containsAny(lower, listOf("help", "what can you do", "capabilities", "commands")) -> {
                RobotIntent(action = RobotAction.HELP, rawText = text)
            }
            lower.contains("status") -> RobotIntent(action = RobotAction.STATUS, rawText = text)
            else -> null
        }
    }

    private fun containsAny(text: String, keywords: List<String>) = keywords.any { text.contains(it) }

    private fun extractSubject(text: String, verbs: List<String>, prepositions: List<String>): String? {
        var result = text
        verbs.forEach { result = result.replace(it, "|") }
        val afterVerb = result.split("|").getOrNull(1)?.trim() ?: return null
        
        var subject = afterVerb
        prepositions.forEach { subject = subject.split(" $it ").first().trim() }
        
        return subject.uppercase().takeIf { it.isNotBlank() }
    }

    private fun extractNumber(text: String): Int? {
        val regex = "\\d+".toRegex()
        return regex.find(text)?.value?.toIntOrNull()
    }

    private fun extractValue(text: String, trigger: String): String? {
        val search = " $trigger "
        if (text.contains(search)) {
            return text.substringAfter(search).split(" ").firstOrNull()?.uppercase()
        }
        return null
    }
}
