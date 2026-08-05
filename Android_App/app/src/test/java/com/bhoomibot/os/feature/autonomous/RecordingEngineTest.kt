package com.bhoomibot.os.feature.autonomous

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bhoomibot.os.data.MissionStorage
import com.bhoomibot.os.model.DriveCommand
import com.bhoomibot.os.feature.manual.ManualUiState
import com.bhoomibot.os.feature.manual.DrivingMode
import com.bhoomibot.os.model.RobotStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Unit tests for [RecordingEngine].
 */
@RunWith(AndroidJUnit4::class)
class RecordingEngineTest {

    private lateinit var context: Context
    private lateinit var engine: RecordingEngine

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        engine = RecordingEngine(context)
        // Clear any existing test missions
        runBlocking {
            MissionStorage.getAllMissions(context).forEach { mission ->
                if (mission.name.startsWith("test_")) {
                    MissionStorage.deleteMission(context, mission.id)
                }
            }
        }
    }

    @Test
    fun `recording engine saves mission with command record`() = runBlocking {
        val testMissionName = "test_mission_${System.currentTimeMillis()}"
        engine.startRecording(testMissionName)

        // Simulate a state change
        val uiState = ManualUiState(
            robotStatus = RobotStatus(),
            drivingMode = DrivingMode.DIGITAL,
            vehicleSpeedPercent = 50,
            ptoEnabled = true,
            ptoSpeedPercent = 50,
            lightsEnabled = true,
            learningEnabled = false,
            hydraulicEnabled = true,
            hydraulicHeightPercent = 30,
            cameraLightEnabled = false,
            lastCommand = DriveCommand.FORWARD,
            bluetoothConnected = true
        )
        engine.recordState(uiState)
        // Wait a bit to ensure the 50ms interval has passed (if we are simulating multiple states)
        delay(60)
        // Record another state to make sure we have at least two
        engine.recordState(uiState.copy(vehicleSpeedPercent = 60))
        delay(60)
        engine.stopRecording()

        // Give it a moment to save
        delay(100)

        // Check that the mission was saved
        val missions = MissionStorage.getAllMissions(context)
        val mission = missions.firstOrNull { it.name == testMissionName }
        assertNotNull("Mission with name $testMissionName should be found", mission)
        val fullMission = MissionStorage.getMission(context, mission!!.id)
        assertNotNull("Mission with id ${mission?.id} should be found", fullMission)
        assertEquals("Should have 2 command records", 2, fullMission!!.rawCommands.size)

        // Check the first command record
        val command = fullMission.rawCommands[0]
        assertEquals(DriveCommand.FORWARD, command.drive)
        assertEquals(50, command.speedPercent)
        assertTrue(command.ptoEnabled)
        assertEquals(50, command.ptoSpeedPercent)
        assertTrue(command.hydraulicEnabled)
        assertEquals(30, command.hydraulicHeightPercent)
        assertTrue(command.lightsEnabled)
        assertFalse(command.cameraLightEnabled)
        assertFalse(command.hornTriggered)
        // GPS and heading are set to 0.0 as placeholder
        assertEquals(0.0, command.latitude, 0.001)
        assertEquals(0.0, command.longitude, 0.001)
        assertEquals(0.0, command.heading, 0.001)
        assertEquals(0.0f, command.gpsAccuracy, 0.001f)
    }

    @Test
    fun `recording engine respects minimum interval`() = runBlocking {
        val testMissionName = "test_interval_${System.currentTimeMillis()}"
        engine.startRecording(testMissionName)

        val uiState = ManualUiState(
            robotStatus = RobotStatus(),
            drivingMode = DrivingMode.DIGITAL,
            vehicleSpeedPercent = 50,
            ptoEnabled = false,
            ptoSpeedPercent = 0,
            lightsEnabled = false,
            learningEnabled = false,
            hydraulicEnabled = false,
            hydraulicHeightPercent = 0,
            cameraLightEnabled = false,
            lastCommand = DriveCommand.STOP,
            bluetoothConnected = true
        )

        // Record first state
        engine.recordState(uiState)
        // Record immediately again (should be ignored due to 50ms minimum interval)
        engine.recordState(uiState)
        delay(20) // Wait less than 50ms
        engine.recordState(uiState) // Still within 50ms of first, should be ignored
        delay(40) // Now total 60ms from first
        engine.recordState(uiState) // This should be recorded (60ms > 50ms)
        delay(60)
        engine.recordState(uiState) // And this one too (120ms > 50ms)
        delay(60)
        engine.stopRecording()

        delay(100)

        val missions = MissionStorage.getAllMissions(context)
        val mission = missions.firstOrNull { it.name == testMissionName }
        assertNotNull(mission)
        val fullMission = MissionStorage.getMission(context, mission!!.id)
        assertNotNull(fullMission)
        // We expect 3 records: first, the one at 60ms, and the one at 120ms
        assertEquals(3, fullMission!!.rawCommands.size)
    }
}
