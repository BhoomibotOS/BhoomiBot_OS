package com.bhoomibot.os.feature.autonomous.simulation.scenario

import com.bhoomibot.os.feature.autonomous.simulation.engine.DigitalTwin
import com.bhoomibot.os.feature.autonomous.simulation.engine.SimEngine
import com.bhoomibot.os.feature.autonomous.simulation.world.FarmWorld
import kotlinx.coroutines.*

/**
 * SCENARIO RUNNER: Executes predefined test cases.
 */
class ScenarioRunner(private val simEngine: SimEngine) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Runs a scenario and injects events into the simulation.
     */
    fun run(scenario: Scenario) {
        // 1. Reset Environment
        DigitalTwin.update(scenario.initialPose)
        FarmWorld.clearWorld()
        simEngine.start()

        // 2. Schedule Events
        scope.launch {
            val startTime = System.currentTimeMillis()
            
            while (isActive) {
                val elapsedSec = (System.currentTimeMillis() - startTime) / 1000
                
                if (elapsedSec >= scenario.durationSeconds) {
                    simEngine.stop()
                    cancel()
                }

                // Find events due at this time
                val dueEvents = scenario.events.filter { 
                    when(it) {
                        is ScenarioEvent.SpawnObstacle -> true // Simpler logic for now
                        else -> false
                    }
                }
                
                // For this MVP, we execute all events at start or based on a simple timer
                // In a full implementation, we'd use a priority queue.
                delay(1000)
            }
        }
    }
}
