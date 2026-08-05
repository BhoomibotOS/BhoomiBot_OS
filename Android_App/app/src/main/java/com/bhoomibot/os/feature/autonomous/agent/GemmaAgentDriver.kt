package com.bhoomibot.os.feature.autonomous.agent

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.bhoomibot.os.feature.autonomous.core.model.Intent
import com.bhoomibot.os.model.DriveCommand
import org.json.JSONObject
import java.io.File
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * FEATURE: L0 Gemma LLM Driver
 * 
 * JUNIOR ENGINEER NOTE: This is the high-intelligence parser. It uses Google's 
 * Gemma-2B model to understand slang and multi-turn context. 
 * If the model file is missing, it falls back to basic logic.
 */
class GemmaAgentDriver(private val context: Context) {

    private var llmInference: LlmInference? = null
    
    // Path where you should place the gemma-2b-it-gpu-int4.bin file
    private val modelPath = "${context.filesDir.absolutePath}/gemma.bin"

    init {
        setupLLM()
    }

    private fun setupLLM() {
        val modelFile = File(modelPath)
        if (!modelFile.exists()) {
            android.util.Log.w("Gemma", "Model file not found at $modelPath. Falling back to heuristic parsing.")
            return
        }

        try {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(512)
                .setTopK(40)
                .setTemperature(0.2f) // Low temperature for deterministic robotic output
                .build()
            
            llmInference = LlmInference.createFromOptions(context, options)
            android.util.Log.i("Gemma", "Gemma-2B initialized successfully.")
        } catch (e: Exception) {
            android.util.Log.e("Gemma", "Failed to initialize LLM", e)
        }
    }

    /**
     * Uses Gemma to parse natural language into a structured JSON Intent.
     * AI-Fix: Injected learned skills into prompt for context-based "training".
     */
    suspend fun parseWithIntelligence(text: String, history: List<String>, learnedSkills: List<String>): Intent? = withContext(Dispatchers.Default) {
        val llm = llmInference ?: return@withContext null

        val systemPrompt = """
            You are BhoomiBot AgentOS Reflex. Parse user intent into JSON.
            Actions: TRANSPORT, NAVIGATE, LEARN, SKILL_REPLAY.
            Known Skills: ${learnedSkills.joinToString(", ")}
            Context: ${history.joinToString(" | ")}
            
            Format: {"action": "...", "subject": "...", "destination": "...", "skillName": "..."}
            User: $text
            Output:
        """.trimIndent()

        try {
            val response = llm.generateResponse(systemPrompt)
            val jsonStr = response.substringAfter("{").substringBeforeLast("}")
            val fullJson = "{$jsonStr}"
            
            val obj = JSONObject(fullJson)
            Intent(
                action = obj.optString("action", "STOP"),
                subject = obj.optString("subject", null),
                source = obj.optString("source", null),
                destination = obj.optString("destination", null),
                repeatCount = obj.optInt("repeatCount", 1),
                rawText = text,
                confidence = 0.95f
            )
        } catch (e: Exception) {
            android.util.Log.e("Gemma", "LLM Generation failed", e)
            null
        }
    }
}
