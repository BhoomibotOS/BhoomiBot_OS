package com.bhoomibot.os.feature.mission

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bhoomibot.os.feature.autonomous.ai.PathGenerator
import com.bhoomibot.os.model.CommandRecord
import com.bhoomibot.os.model.MissionRecord
import com.bhoomibot.os.model.MissionMetadata
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MissionLibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MissionLibraryRepository(application.applicationContext)
    private val playbackEngine = PlaybackEngine()

    private val _uiState = MutableStateFlow(MissionLibraryUiState())
    val uiState: StateFlow<MissionLibraryUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadMissions()
    }

    /** Load all saved missions */
    fun loadMissions() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val missions = repository.getAllMissions()
                _uiState.value = _uiState.value.copy(
                    missions = missions,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load missions: ${e.message}"
                )
            }
        }
    }

    /** Load full mission details by ID */
    fun loadMissionDetails(missionId: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val mission = repository.getMission(missionId)
                _uiState.value = _uiState.value.copy(
                    selectedMission = mission,
                    isLoading = false,
                    isPlaybackReady = mission != null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load mission: ${e.message}"
                )
            }
        }
    }

    /** Delete a mission by ID */
    fun deleteMission(missionId: String) {
        viewModelScope.launch {
            try {
                repository.deleteMission(missionId)
                // Refresh the list
                val missions = repository.getAllMissions()
                _uiState.value = _uiState.value.copy(
                    missions = missions,
                    selectedMission = null,
                    isPlaybackReady = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to delete mission: ${e.message}"
                )
            }
        }
    }

    /** Select a mission for playback */
    fun selectForPlayback(missionId: String) {
        loadMissionDetails(missionId)
    }

    /** AI Expansion: Generate full field coverage from a single pass */
    fun expandMission(template: MissionRecord, width: Double, passes: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val fullMission = PathGenerator.generateFieldCoverage(template, width, passes)
                repository.saveMission(fullMission)
                loadMissions() // Refresh list
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    selectedMission = fullMission
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "AI Expansion failed: ${e.message}"
                )
            }
        }
    }

    /** Clear the selected mission */
    fun clearSelection() {
        _uiState.value = _uiState.value.copy(
            selectedMission = null,
            isPlaybackReady = false
        )
    }
}