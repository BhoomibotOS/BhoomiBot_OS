package com.bhoomibot.ai

import com.bhoomibot.ai.agent.AgentDriver
import com.bhoomibot.ai.agent.BrainResponse
import com.bhoomibot.ai.world.DigitalTwin
import com.bhoomibot.sdk.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * High-level service that manages the autonomous lifecycle.
 * Runs on Laptop or Phone.
 */
class BrainService(
    private val agent: MasterBrain,
    private val world: DigitalTwin
) {
    private val scope = CoroutineScope(Dispatchers.Default)

    /** 
     * Process a raw input (Voice or Text) and return a 
     * robot-executable task plan.
     */
    suspend fun handleCommand(text: String): BrainResponse {
        return agent.processCommand(text)
    }

    /** 
     * Updates the robot's state from physical sensors.
     */
    fun syncPhysicalState(pose: RobotPose, observations: List<Observation>) {
        world.updatePose(pose)
        world.updateObservations(observations)
    }
}
