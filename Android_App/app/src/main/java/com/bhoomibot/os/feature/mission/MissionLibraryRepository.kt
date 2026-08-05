package com.bhoomibot.os.feature.mission

import android.content.Context
import com.bhoomibot.os.data.MissionStorage
import com.bhoomibot.os.model.MissionMetadata
import com.bhoomibot.os.model.MissionRecord
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MissionLibraryRepository(private val context: Context) {

    /** Save mission record */
    suspend fun saveMission(mission: MissionRecord) {
        MissionStorage.saveMission(context, mission)
    }

    /** Retrieve mission by ID */
    suspend fun getMission(missionId: String): MissionRecord? {
        return MissionStorage.getMission(context, missionId)
    }

    /** List all missions */
    suspend fun getAllMissions(): List<MissionMetadata> {
        return MissionStorage.getAllMissions(context)
    }

    /** Delete mission by ID */
    suspend fun deleteMission(missionId: String) {
        MissionStorage.deleteMission(context, missionId)
    }

    /** Check if mission exists */
    suspend fun missionExists(missionId: String): Boolean {
        return MissionStorage.missionExists(context, missionId)
    }
}