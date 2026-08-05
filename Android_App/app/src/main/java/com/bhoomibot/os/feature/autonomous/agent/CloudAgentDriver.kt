package com.bhoomibot.os.feature.autonomous.agent

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * FEATURE: L0 Cloud AI Wisdom Brain (Groq Llama 3 70B)
 * 
 * Provides deep reasoning, agricultural expertise, and advanced intent parsing
 * when the local model is insufficient.
 */
class CloudAgentDriver {

    private val API_KEY = "My key available in one note"
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Queries the Cloud AI for deep reasoning or agricultural knowledge.
     */
    suspend fun queryWisdom(text: String, history: List<ChatMessage>, robotState: String): String? = suspendCancellableCoroutine { continuation ->
        val messages = JSONArray()
        
        // 1. System Prompt: Set the identity and domain knowledge
        messages.put(JSONObject().apply {
            put("role", "system")
            put("content", """
                You are BhoomiBot AgentOS Wisdom, a genius agricultural AI assistant.
                Robot Status: $robotState
                
                Guidelines:
                1. If the user asks a question, answer expertly (farming, mechanics, science).
                2. If the user gives a complex command, suggest logical steps.
                3. Keep answers concise, helpful, and specific to Indian agriculture when applicable.
                4. Do NOT hallucinate data.
            """.trimIndent())
        })

        // 2. Add history for context awareness
        history.takeLast(5).forEach { msg ->
            messages.put(JSONObject().apply {
                put("role", if (msg.isUser) "user" else "assistant")
                put("content", msg.text)
            })
        }

        // 3. Add current prompt
        messages.put(JSONObject().apply {
            put("role", "user")
            put("content", text)
        })

        val requestBody = JSONObject().apply {
            put("model", "llama3-70b-8192")
            put("messages", messages)
            put("temperature", 0.7)
        }.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("https://api.groq.com/openai/v1/chat/completions")
            .addHeader("Authorization", "Bearer $API_KEY")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("CloudAI", "Request failed", e)
                if (continuation.isActive) continuation.resume(null)
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (response.isSuccessful && body != null) {
                    try {
                        val json = JSONObject(body)
                        val content = json.getJSONArray("choices")
                            .getJSONObject(0)
                            .getJSONObject("message")
                            .getString("content")
                        if (continuation.isActive) continuation.resume(content)
                    } catch (e: Exception) {
                        Log.e("CloudAI", "JSON Parsing failed", e)
                        if (continuation.isActive) continuation.resume(null)
                    }
                } else {
                    Log.e("CloudAI", "Error response: ${response.code} $body")
                    if (continuation.isActive) continuation.resume(null)
                }
            }
        })
    }
}
