package com.bhoomibot.os.feature.autonomous.skills.library

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.bhoomibot.os.feature.autonomous.skills.models.DemonstratedSkill
import com.bhoomibot.os.feature.autonomous.skills.models.ExperienceDelta
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore(name = "skill_library")

/**
 * FEATURE: Skill Library (L3)
 * 
 * JUNIOR ENGINEER NOTE: This is the permanent database for all robot skills.
 * It stores logical workflows learned from demonstrations.
 */
object SkillLibrary {

    private val SKILLS_KEY = stringPreferencesKey("learned_skills")

    suspend fun saveSkill(context: Context, skill: DemonstratedSkill) {
        val currentSkills = getAllSkills(context).toMutableList()
        currentSkills.removeAll { it.name == skill.name } // Overwrite if exists
        currentSkills.add(skill)
        
        context.dataStore.edit { prefs ->
            prefs[SKILLS_KEY] = Json.encodeToString(currentSkills)
        }
    }

    suspend fun getAllSkills(context: Context): List<DemonstratedSkill> {
        val prefs = context.dataStore.data.first()
        val json = prefs[SKILLS_KEY] ?: return emptyList()
        return try {
            Json.decodeFromString(json)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getSkillByName(context: Context, name: String): DemonstratedSkill? {
        return getAllSkills(context).find { it.name.equals(name, ignoreCase = true) }
    }

    /**
     * Applies a human correction to a specific step in a skill.
     */
    suspend fun applyCorrection(context: Context, delta: ExperienceDelta) {
        val allSkills = getAllSkills(context).toMutableList()
        val skillIndex = allSkills.indexOfFirst { it.id == delta.skillId }
        
        if (skillIndex != -1) {
            val skill = allSkills[skillIndex]
            val updatedSteps = skill.steps.toMutableList()
            val stepIndex = updatedSteps.indexOfFirst { it.sequence == delta.stepSequence }
            
            if (stepIndex != -1) {
                val oldStep = updatedSteps[stepIndex]
                updatedSteps[stepIndex] = oldStep.copy(
                    latitude = delta.correctedLatitude,
                    longitude = delta.correctedLongitude,
                    parameters = oldStep.parameters + delta.correctedParameters
                )
                
                allSkills[skillIndex] = skill.copy(steps = updatedSteps)
                
                context.dataStore.edit { prefs ->
                    prefs[SKILLS_KEY] = Json.encodeToString(allSkills)
                }
                android.util.Log.i("SkillLibrary", "Applied correction to ${skill.name} step ${delta.stepSequence}")
            }
        }
    }
}
