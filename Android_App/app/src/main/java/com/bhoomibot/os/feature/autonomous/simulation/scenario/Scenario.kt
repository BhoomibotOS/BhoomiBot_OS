package com.bhoomibot.os.feature.autonomous.simulation.scenario

import com.bhoomibot.os.feature.autonomous.simulation.engine.RobotStateVector

/**
 * SCENARIO: A predefined test case for the robot.
 */
data class Scenario(
    val name: String,
    val initialPose: RobotStateVector,
    val events: List<ScenarioEvent>,
    val durationSeconds: Int = 60
)

/**
 * SCENARIO EVENT: Something that happens during a test.
 */
sealed class ScenarioEvent {
    data class SpawnObstacle(val x: Double, val y: Double, val label: String) : ScenarioEvent()
    data class LossOfGps(val startTimeSec: Int, val durationSec: Int) : ScenarioEvent()
    data class UserCommand(val timeSec: Int, val command: String) : ScenarioEvent()
}
