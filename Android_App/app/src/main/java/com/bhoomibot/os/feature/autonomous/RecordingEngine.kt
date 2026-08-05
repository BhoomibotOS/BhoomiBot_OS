package com.bhoomibot.os.feature.autonomous

import android.content.Context
import com.bhoomibot.os.connection.model.RobotCommand
import com.bhoomibot.os.data.LocationTracker
import com.bhoomibot.os.data.MissionStorage
import com.bhoomibot.os.feature.manual.ManualUiState
import com.bhoomibot.os.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Collections
import java.util.UUID

/**
 * Records manual driving operations into a mission.
 */
class RecordingEngine(
    private val context: Context,
    private val stateMachine: AutonomyStateMachine? = null
) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val locationTracker = LocationTracker(context)

    private var currentMissionName: String = "Unnamed Mission"
    private val recordedCommands = Collections.synchronizedList(mutableListOf<CommandRecord>())
    private val recordedWaypoints = Collections.synchronizedList(mutableListOf<Waypoint>())
    private var lastRecordTime: Long = 0L
    private val minIntervalMs = 50L

    fun startRecording(missionName: String = "Unnamed Mission") {
        val success = stateMachine?.transitionTo(AutonomyState.RECORDING) ?: true
        if (!success) return

        currentMissionName = missionName
        recordedCommands.clear()
        recordedWaypoints.clear()
        lastRecordTime = 0L
        locationTracker.startTracking()
    }

    fun recordState(uiState: ManualUiState, marker: String? = null) {
        if (!isRecording()) return

        val now = System.currentTimeMillis()
        if (marker == null && now - lastRecordTime < minIntervalMs) return

        val loc = locationTracker.currentLocation.value
        
        val record = CommandRecord(
            drive = uiState.lastCommand,
            speedPercent = uiState.vehicleSpeedPercent,
            ptoEnabled = uiState.ptoEnabled,
            timestamp = now,
            latitude = loc?.latitude ?: 0.0,
            longitude = loc?.longitude ?: 0.0,
            heading = loc?.bearing?.toDouble() ?: 0.0,
            gpsAccuracy = loc?.accuracy ?: 0.0f,
            marker = marker
        )
        addRecord(record, loc)
    }

    fun addMarker(markerType: String) {
        if (!isRecording()) return
        val now = System.currentTimeMillis()
        val loc = locationTracker.currentLocation.value
        
        val record = CommandRecord(
            timestamp = now,
            latitude = loc?.latitude ?: 0.0,
            longitude = loc?.longitude ?: 0.0,
            marker = markerType
        )
        addRecord(record, loc)
    }

    private fun addRecord(record: CommandRecord, loc: android.location.Location?) {
        recordedCommands.add(record)
        if (loc != null) {
            recordedWaypoints.add(Waypoint(
                latitude = loc.latitude,
                longitude = loc.longitude,
                timestamp = record.timestamp,
                accuracy = loc.accuracy
            ))
        }
        lastRecordTime = record.timestamp
    }

    fun stopRecording(): MissionRecord {
        locationTracker.stopTracking()

        val mission = MissionRecord(
            id = UUID.randomUUID().toString(),
            name = currentMissionName,
            waypoints = recordedWaypoints.toList(),
            rawCommands = recordedCommands.toList(),
            operatorId = "operator_1",
            createdTimestamp = System.currentTimeMillis()
        )

        scope.launch {
            MissionStorage.saveMission(context, mission)
            stateMachine?.transitionTo(AutonomyState.MISSION_SAVED)
        }

        return mission
    }

    fun isRecording(): Boolean {
        return stateMachine?.state?.value == AutonomyState.RECORDING
    }

    suspend fun getAllMissions(): List<MissionMetadata> = MissionStorage.getAllMissions(context)
    
    fun recordCommand(command: RobotCommand) {
        if (!isRecording()) return
        val now = System.currentTimeMillis()
        if (now - lastRecordTime < minIntervalMs) return
        val loc = locationTracker.currentLocation.value

        val record = CommandRecord(
            drive = command.drive,
            speedPercent = command.speedPercent,
            ptoEnabled = command.pto ?: false,
            timestamp = now,
            latitude = loc?.latitude ?: 0.0,
            longitude = loc?.longitude ?: 0.0,
            marker = null
        )
        addRecord(record, loc)
    }
}
