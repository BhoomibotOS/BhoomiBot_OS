package com.bhoomibot.os.feature.autonomous.skills.learning

import android.content.Context
import com.bhoomibot.os.feature.autonomous.AutonomyManager
import com.bhoomibot.os.feature.autonomous.skills.library.SkillLibrary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * FEATURE: Teaching Manager
 * 
 * Orchestrates the "Skill-by-Demonstration" workflow.
 */
object TeachingManager {

    private var activeSkillName: String? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    fun startTeaching(context: Context, skillName: String) {
        activeSkillName = skillName
        val recorder = AutonomyManager.getRecordingEngine(context)
        recorder.startRecording("Demo: $skillName")
    }

    fun finishTeaching(context: Context) {
        val name = activeSkillName ?: return
        val recorder = AutonomyManager.getRecordingEngine(context)
        
        val rawRecord = recorder.stopRecording()
        
        // --- ASYNCHRONOUS ANALYSIS ---
        scope.launch {
            // 1. Analyze raw data into logical steps
            val skill = EventAnalyzer.analyzeDemonstration(name, rawRecord)
            
            // 2. Save to library
            SkillLibrary.saveSkill(context, skill)
            
            android.util.Log.i("TeachingManager", "Successfully learned skill: ${skill.name} with ${skill.steps.size} steps")
            activeSkillName = null
        }
    }

    fun addActionMarker(context: Context, marker: String) {
        val recorder = AutonomyManager.getRecordingEngine(context)
        recorder.addMarker(marker)
    }
}
