package com.bhoomibot.os.feature.autonomous.agent

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bhoomibot.os.feature.autonomous.core.AgentOSManager
import com.bhoomibot.os.feature.autonomous.core.interfaces.AgentResponse
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatMessage(val text: String, val isUser: Boolean)

sealed class AgentNavigation {
    object ToManual : AgentNavigation()
    data class ToPlayback(val missionId: String) : AgentNavigation()
}

enum class AgentConvState { IDLE, WAITING_FOR_SKILL_NAME }

class AgentViewModel(application: Application) : AndroidViewModel(application) {

    private val manager = AgentOSManager(application)
    private val cloudBrain = CloudAgentDriver()
    private var currentState = AgentConvState.IDLE

    private val _messages = MutableStateFlow<List<ChatMessage>>(listOf(
        ChatMessage("BhoomiBot AgentOS Online. I am your autonomous agricultural partner.", false),
        ChatMessage("You can ask me to:\n• Teach a new skill\n• Run a learned skill\n• Answer farming questions\n• Check robot status", false)
    ))
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _suggestions = MutableStateFlow<List<String>>(listOf(
        "Help / Commands", "Teach a new skill", "Skill Library", "Robot Status"
    ))
    val suggestions: StateFlow<List<String>> = _suggestions.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing = _isProcessing.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening = _isListening.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<AgentNavigation>()
    val navigationEvents = _navigationEvents.asSharedFlow()

    fun startVoiceInput() { _isListening.value = true }
    fun stopVoiceInput() { _isListening.value = false }

    fun sendCommand(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _messages.value += ChatMessage(text, true)
            _isProcessing.value = true
            
            val lower = text.lowercase().trim()
            android.util.Log.d("AgentAI", "Command received in VM: $text")

            if (lower == "help" || lower == "commands" || lower == "help / commands" || lower == "what can you do?") {
                showHelp()
                _isProcessing.value = false
                return@launch
            }

            val processedText = if (currentState == AgentConvState.WAITING_FOR_SKILL_NAME) {
                "Teach skill $text" 
            } else {
                text
            }
            
            val response = manager.executeNaturalCommand(processedText)
            android.util.Log.d("AgentAI", "Local Manager response: $response")
            
            // AI-Fix: Always fallback to Cloud Brain if the local model didn't produce a Plan or StartTeaching
            val needsCloud = response is AgentResponse.Feedback && 
                (response.message.contains("haven't learned") || response.message.contains("I am BhoomiBot"))

            if (needsCloud) {
                android.util.Log.d("AgentAI", "Local understanding failed. Querying Cloud Wisdom...")
                val wisdom = cloudBrain.queryWisdom(text, _messages.value, manager.knowledge.getKnowledgeContext().value.toString())
                if (wisdom != null) {
                    _messages.value += ChatMessage(wisdom, false)
                    _suggestions.value = listOf("Teach this", "Status", "Clear Chat")
                } else {
                    handleAgentResponse(response)
                }
            } else {
                handleAgentResponse(response)
            }
            _isProcessing.value = false
        }
    }

    private fun showHelp() {
        val helpText = """
            I am designed to manage field operations. Here is what I can do:
            
            1. 🎓 TEACHING: Say "Teach me a skill" to record a new behavior.
            2. ⚡ EXECUTION: Say "Run [Skill Name]" to perform a learned task.
            3. 🌾 EXPERTISE: Ask questions like "How to treat leaf rust?"
            4. 📊 STATUS: Say "Status" to check battery, GPS, and VCU.
            5. 🗣️ CHAT: I understand slang like "hru" or "what's up".
        """.trimIndent()
        _messages.value += ChatMessage(helpText, false)
        _suggestions.value = listOf("Teach a new skill", "Skill Library", "Status")
    }

    private fun handleAgentResponse(response: AgentResponse) {
        // Reset state unless explicitly set
        val oldState = currentState
        currentState = AgentConvState.IDLE
        
        when (response) {
            is AgentResponse.PlanReady -> {
                _messages.value += ChatMessage("Plan created successfully. Switching to execution view.", false)
                _suggestions.value = listOf("Pause Mission", "Stop Robot", "Status")
                viewModelScope.launch {
                    _navigationEvents.emit(AgentNavigation.ToPlayback(response.plan.id))
                }
            }
            is AgentResponse.StartTeaching -> {
                _messages.value += ChatMessage("Understood. I'm ready to learn ${response.skillName}. Let's go to manual mode!", false)
                _suggestions.value = listOf("Finish", "Add Marker", "Cancel")
                viewModelScope.launch {
                    _navigationEvents.emit(AgentNavigation.ToManual)
                }
            }
            is AgentResponse.ClarificationNeeded -> {
                _messages.value += ChatMessage(response.query, false)
                _suggestions.value = response.options
            }
            is AgentResponse.Feedback -> {
                _messages.value += ChatMessage(response.message, false)
                if (response.message.contains("What should I call")) {
                    currentState = AgentConvState.WAITING_FOR_SKILL_NAME
                    _suggestions.value = listOf("Cancel")
                } else if (oldState == AgentConvState.IDLE) {
                    _suggestions.value = listOf("Teach a new skill", "Go to Shed", "Status")
                }
            }
        }
    }
}
