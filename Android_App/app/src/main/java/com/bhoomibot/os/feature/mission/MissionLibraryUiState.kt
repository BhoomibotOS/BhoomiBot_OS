package com.bhoomibot.os.feature.mission

import com.bhoomibot.os.model.MissionMetadata
import com.bhoomibot.os.model.MissionRecord

data class MissionLibraryUiState(
    val missions: List<MissionMetadata> = emptyList(),
    val selectedMission: MissionRecord? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isPlaybackReady: Boolean = false
)