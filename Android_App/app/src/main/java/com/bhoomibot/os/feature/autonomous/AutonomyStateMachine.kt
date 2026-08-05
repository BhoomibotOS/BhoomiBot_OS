package com.bhoomibot.os.feature.autonomous

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.util.Log

/**
 * Manages transitions between autonomy states.
 * 
 * Ensures that the system only moves between valid states as defined in the
 * architecture. Provides a single source of truth for what the autonomy
 * system is currently doing.
 */
class AutonomyStateMachine {

    private val _state = MutableStateFlow(AutonomyState.IDLE)
    val state: StateFlow<AutonomyState> = _state.asStateFlow()

    /**
     * Attempts to transition to the next state.
     * Returns true if transition was valid and successful.
     */
    fun transitionTo(newState: AutonomyState): Boolean {
        val currentState = _state.value
        
        // E-STOP is always allowed from any state
        if (newState == AutonomyState.EMERGENCY_STOP) {
            _state.value = newState
            return true
        }

        val isValid = when (currentState) {
            AutonomyState.IDLE -> newState == AutonomyState.RECORDING || newState == AutonomyState.READY
            AutonomyState.RECORDING -> newState == AutonomyState.MISSION_SAVED || newState == AutonomyState.ERROR
            AutonomyState.MISSION_SAVED -> newState == AutonomyState.READY || newState == AutonomyState.IDLE
            AutonomyState.READY -> newState == AutonomyState.EXECUTING || newState == AutonomyState.IDLE
            AutonomyState.EXECUTING -> newState == AutonomyState.PAUSED || newState == AutonomyState.COMPLETED || newState == AutonomyState.ERROR
            AutonomyState.PAUSED -> newState == AutonomyState.EXECUTING || newState == AutonomyState.ERROR || newState == AutonomyState.IDLE
            AutonomyState.COMPLETED -> newState == AutonomyState.IDLE || newState == AutonomyState.READY
            AutonomyState.ERROR -> newState == AutonomyState.IDLE
            AutonomyState.EMERGENCY_STOP -> newState == AutonomyState.IDLE // Requires manual reset to IDLE
        }

        return if (isValid) {
            Log.d("AutonomySM", "Transition: $currentState -> $newState")
            _state.value = newState
            true
        } else {
            Log.w("AutonomySM", "Invalid transition attempt: $currentState -> $newState")
            false
        }
    }

    /**
     * Resets the state machine to IDLE.
     */
    fun reset() {
        _state.value = AutonomyState.IDLE
    }
}
