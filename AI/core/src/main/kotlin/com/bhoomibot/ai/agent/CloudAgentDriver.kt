package com.bhoomibot.ai.agent

import com.bhoomibot.sdk.RobotIntent
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
 * L0 Cloud AI Driver (Groq Llama 3)
 */
class CloudAgentDriver(private val apiKey: String) : AgentDriver {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    override suspend fun parseIntent(text: String, learnedSkills: List<String>): RobotIntent? {
        val response = queryGroq(text, learnedSkills) ?: return null
        return try {
            val json = JSONObject(response)
            RobotIntent(
                action = com.bhoomibot.sdk.RobotAction.valueOf(json.optString("action", "STOP")),
                subject = json.optString("subject", null),
                source = json.optString("source", null),
                destination = json.optString("destination", null),
                repeatCount = json.optInt("repeatCount", 1),
                rawText = text
            )
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun queryGroq(text: String, skills: List<String>): String? = suspendCancellableCoroutine { continuation ->
        val messages = JSONArray()
        messages.put(JSONObject().apply {
            put("role", "system")
            put("content", """
                You are BhoomiBot AgentOS, an autonomous robot controller. 
                Convert user requests into a JSON object with: 
                - action: (NAVIGATE, TRANSPORT, STATUS, STOP, LEARN, HELP)
                - subject: (the item to move, if any)
                - source: (start location, if any)
                - destination: (end location, if any)
                - repeatCount: (number of times, default 1)
                
                Known Skills: ${skills.joinToString(", ")}
            """.trimIndent())
        })
        messages.put(JSONObject().apply {
            put("role", "user")
            put("content", text)
        })

        val body = JSONObject().apply {
            put("model", "llama3-70b-8192")
            put("messages", messages)
            put("response_format", JSONObject().put("type", "json_object"))
        }.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url("https://api.groq.com/openai/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { continuation.resume(null) }
            override fun onResponse(call: Call, response: Response) {
                val resBody = response.body?.string()
                if (response.isSuccessful && resBody != null) {
                    val content = JSONObject(resBody).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
                    continuation.resume(content)
                } else {
                    continuation.resume(null)
                }
            }
        })
    }
}
