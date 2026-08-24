package com.bhoomibot.os.feature.autonomous.agent

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bhoomibot.os.feature.autonomous.AutonomyManager
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
    data class ToPlayback(val missionName: String, val plan: com.bhoomibot.sdk.TaskPlan? = null) : AgentNavigation()
}

enum class AgentConvState { IDLE, WAITING_FOR_SKILL_NAME }

class AgentViewModel(application: Application) : AndroidViewModel(application) {

    private val manager = AutonomyManager.getAgentOS(application)
    private var currentState = AgentConvState.IDLE

    private val _messages = MutableStateFlow<List<ChatMessage>>(listOf(
        ChatMessage("BhoomiBot AgentOS Online. I am your autonomous agricultural partner.", false),
        ChatMessage("Try saying:\n• 'Go to the field'\n• 'Carry Fertilizer from Shed to Home'\n• 'Teach a new skill'\n• 'Check robot status'", false)
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
            
            val processedText = if (currentState == AgentConvState.WAITING_FOR_SKILL_NAME) "Teach skill $text" else text
            
            val response = manager.executeNaturalCommand(processedText)
            handleAgentResponse(response)
            
            _isProcessing.value = false
        }
    }

    private fun handleAgentResponse(response: AgentResponse) {
        currentState = AgentConvState.IDLE
        
        when (response) {
            is AgentResponse.PlanReady -> {
                _messages.value += ChatMessage("Plan created successfully. Switching to execution view.", false)
                com.bhoomibot.os.feature.autonomous.AutonomyManager.pendingPlan = response.plan
                viewModelScope.launch {
                    _navigationEvents.emit(AgentNavigation.ToPlayback("PLAN_EXECUTION", response.plan))
                }
            }
            is AgentResponse.StartTeaching -> {
                _messages.value += ChatMessage("Understood. ready to learn ${response.skillName}. Let's go to manual mode!", false)
                viewModelScope.launch {
                    _navigationEvents.emit(AgentNavigation.ToManual)
                }
            }
            is AgentResponse.Feedback -> {
                _messages.value += ChatMessage(response.message, false)
                if (response.message.contains("What should I call")) {
                    currentState = AgentConvState.WAITING_FOR_SKILL_NAME
                }
            }
            is AgentResponse.ClarificationNeeded -> {
                _messages.value += ChatMessage(response.query, false)
                _suggestions.value = response.options
            }
        }
    }
}
