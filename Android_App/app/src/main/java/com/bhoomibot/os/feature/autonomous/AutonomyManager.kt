package com.bhoomibot.os.feature.autonomous

import android.app.Application
import android.content.Context
import com.bhoomibot.os.connection.repository.LiveLinkRepositoryProvider
import com.bhoomibot.os.feature.autonomous.ai.PerceptionEngine
import com.bhoomibot.os.feature.mission.PlaybackEngine
import com.bhoomibot.os.repository.provideRobotRepository

/**
 * AutonomyManager: The global coordinator for all autonomous functions.
 * 
 * JUNIOR ENGINEER NOTE: This is an "Object" (Singleton). It ensures that both the
 * RECORDING and PLAYBACK engines share the same state and sensors. 
 * This prevents the robot from trying to record and replay at the same time.
 */
object AutonomyManager {

    // The single source of truth for the robot's current state (IDLE, RECORDING, etc.)
    val stateMachine = AutonomyStateMachine()
    
    private var _recordingEngine: RecordingEngine? = null
    private var _playbackEngine: PlaybackEngine? = null
    private var _safetyMonitor: SafetyMonitor? = null
    private var _perceptionEngine: PerceptionEngine? = null

    /** 
     * Returns the engine responsible for saving manual driving data to the local database.
     * Matches the "Learning" button in the Manual Control screen.
     */
    fun getRecordingEngine(context: Context): RecordingEngine {
        if (_recordingEngine == null) {
            _recordingEngine = RecordingEngine(context.applicationContext, stateMachine)
        }
        return _recordingEngine!!
    }

    /** 
     * Returns the engine responsible for replaying saved missions.
     * Matches the "START REPLAY" button in the Mission Library.
     */
    fun getPlaybackEngine(application: Application): PlaybackEngine {
        if (_playbackEngine == null) {
            val repo = provideRobotRepository(application)
            val perception = getPerceptionEngine(application)
            _playbackEngine = PlaybackEngine(repo, stateMachine = stateMachine, perceptionEngine = perception)
        }
        return _playbackEngine!!
    }

    /** 
     * Returns the background monitor that watches for stalls (Smart Stall Detection).
     */
    fun getSafetyMonitor(application: Application): SafetyMonitor {
        if (_safetyMonitor == null) {
            val liveRepo = LiveLinkRepositoryProvider.get(application)
            _safetyMonitor = SafetyMonitor(application, stateMachine, liveRepo)
            _safetyMonitor!!.startMonitoring()
        }
        return _safetyMonitor!!
    }

    /** 
     * Returns the AI Vision system (TensorFlow / CV) used to see crop rows.
     */
    fun getPerceptionEngine(context: Context): PerceptionEngine {
        if (_perceptionEngine == null) {
            _perceptionEngine = PerceptionEngine(context.applicationContext)
        }
        return _perceptionEngine!!
    }
}
