package com.bhoomibot.os.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.stringPreferencesKey
import com.bhoomibot.os.model.MissionRecord
import com.bhoomibot.os.model.MissionMetadata
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val STORAGE_NAME = "auto_missions"
private val Context.missionStore by preferencesDataStore(name = STORAGE_NAME)

object MissionStorage {
    /** Save mission record under its ID key */
    suspend fun saveMission(context: Context, mission: MissionRecord) {
        context.missionStore.edit { preferences ->
            preferences[stringPreferencesKey("mission_${mission.id}")] = Json.encodeToString(mission)
        }
    }

    /** Retrieve mission by ID */
    suspend fun getMission(context: Context, missionId: String): MissionRecord? {
        val key = "mission_$missionId"
        val preferences = context.missionStore.data.first()
        val json = preferences[stringPreferencesKey(key)]
        return if (json != null) Json.decodeFromString<MissionRecord>(json) else null
    }

    /** List all missions */
    suspend fun getAllMissions(context: Context): List<MissionMetadata> {
        val preferences = context.missionStore.data.first()
        return preferences.asMap()
            .filter { (key, _) ->
                key.name.startsWith("mission_")
            }
            .mapNotNull { (_, value) ->
                val json = value as? String
                if (json != null && json.isNotEmpty()) {
                    try {
                        val record = Json.decodeFromString<MissionRecord>(json)
                        MissionMetadata(
                            id = record.id,
                            name = record.name,
                            durationSeconds = record.rawCommands.size * 2,
                            waypointCount = record.waypoints.size,
                            commandCount = record.rawCommands.size
                        )
                    } catch (e: Exception) {
                        null
                    }
                } else null
            }
    }

    /** Delete mission by ID */
    suspend fun deleteMission(context: Context, missionId: String) {
        context.missionStore.edit { preferences ->
            preferences.remove(stringPreferencesKey("mission_$missionId"))
        }
    }

    /** Check if mission exists */
    suspend fun missionExists(context: Context, missionId: String): Boolean {
        val preferences = context.missionStore.data.first()
        return preferences.contains(stringPreferencesKey("mission_$missionId"))
    }
}
