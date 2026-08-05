package com.bhoomibot.os.feature.autonomous.agent

import com.bhoomibot.os.feature.autonomous.core.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * FEATURE: Cognitive Dialogue Manager
 * 
 * AI-Fix: Optimized for Indian English agricultural phrasing.
 * Handles common regional structures like "Go to well side" or "Shift load to shed".
 */
class DialogueManager {

    private val _state = MutableStateFlow(DialogueState())
    val state = _state.asStateFlow()

    /**
     * Maps natural language into a structured Intent while considering context.
     */
    fun parseNaturalLanguage(text: String): Intent? {
        val lower = text.lowercase(Locale.ROOT).trim()
        val currentState = _state.value

        // 1. Handle Contextual References ("Do it again")
        if (lower == "do it again" || lower == "one more time" || lower == "repeat") {
            return currentState.lastIntent
        }

        // 2. Handle Elliptical References ("Go there")
        if (lower.contains("go there") || lower.contains("drive there") || lower.contains("take it there")) {
            val lastDest = currentState.lastIntent?.destination
            if (lastDest != null) {
                return Intent(action = "NAVIGATE", subject = null, destination = lastDest, rawText = text, confidence = 0.9f)
            }
        }

        // 3. Indian English / Regional Ag-Mapping
        val intent = when {
            // Task: Transport ("Shift", "Take", "Put", "Move", "Carry")
            containsAny(lower, listOf("shift", "take", "put", "place", "move", "carry", "load")) -> {
                val to = extractValue(lower, listOf("to", "at", "towards", "near"))
                val from = extractValue(lower, listOf("from", "starting at"))
                
                // Clean up suffixes like "side" (e.g., "Well side" -> "Well")
                val cleanTo = to?.removeSuffix(" SIDE")?.removeSuffix(" NEAR")
                
                Intent(action = "TRANSPORT", subject = "LOAD", source = from, destination = cleanTo, rawText = text, confidence = 0.85f)
            }
            
            // Task: Simple Navigation ("Go to", "Drive to", "Reach")
            containsAny(lower, listOf("go to", "drive to", "reach", "head to")) -> {
                val dest = extractValue(lower, listOf("to", "the", "at"))
                val cleanDest = dest?.removeSuffix(" SIDE")?.removeSuffix(" NEAR")
                Intent(action = "NAVIGATE", subject = null, destination = cleanDest, rawText = text, confidence = 0.85f)
            }

            lower == "status" || lower.contains("robot status") -> {
                Intent(action = "STATUS", subject = "SYSTEM", destination = null, rawText = text, confidence = 1.0f)
            }

            else -> null
        }

        // Update context if we found a valid intent
        if (intent != null) {
            _state.value = currentState.copy(lastIntent = intent, history = currentState.history + text)
        }

        return intent
    }

    private fun containsAny(text: String, keywords: List<String>): Boolean {
        return keywords.any { text.contains(it) }
    }

    private fun extractValue(text: String, triggers: List<String>): String? {
        for (trigger in triggers) {
            val search = " $trigger "
            if (text.contains(search)) {
                val value = text.substringAfter(search).split(" ").firstOrNull()
                return value?.uppercase(Locale.ROOT)
            }
            // Also check if it's at the end of the sentence
            if (text.endsWith(" $trigger")) {
                 return null
            }
        }
        
        // Handle simple "Go to [Place]" where place is the last word
        if (text.contains(" to ")) {
            val afterTo = text.substringAfter(" to ").trim()
            if (afterTo.isNotEmpty()) return afterTo.uppercase(Locale.ROOT)
        }

        return null
    }
}
