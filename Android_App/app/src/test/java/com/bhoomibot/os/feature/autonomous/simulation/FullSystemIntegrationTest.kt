package com.bhoomibot.os.feature.autonomous.simulation

import android.app.Application
import com.bhoomibot.os.feature.autonomous.agent.AgentLayerImpl
import com.bhoomibot.os.feature.autonomous.core.interfaces.AgentResponse
import com.bhoomibot.os.feature.autonomous.core.model.KnowledgeNode
import com.bhoomibot.os.feature.autonomous.core.model.NodeType
import com.bhoomibot.os.feature.autonomous.knowledge.KnowledgeLayerImpl
import com.bhoomibot.os.feature.autonomous.planning.PlannerLayerImpl
import com.bhoomibot.os.feature.autonomous.simulation.engine.DigitalTwin
import com.bhoomibot.os.feature.autonomous.simulation.engine.SimEngine
import com.bhoomibot.os.feature.autonomous.simulation.hardware.MockRobotRepository
import com.bhoomibot.os.feature.autonomous.simulation.world.FarmWorld
import com.bhoomibot.os.feature.autonomous.simulation.hardware.MockPerception
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class FullSystemIntegrationTest {

    @Test
    fun runFullEndToEndSimulation() = runBlocking {
        println("=== STARTING FULL SYSTEM INTEGRATION TEST ===")
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        
        // 1. Initialize 9-Layer Components
        val knowledge = KnowledgeLayerImpl(context)
        val planner = PlannerLayerImpl()
        val agent = AgentLayerImpl(knowledge, planner)
        val simEngine = SimEngine(frequencyHz = 10)
        val robotRepo = MockRobotRepository()
        val perception = MockPerception()
        
        // Reset State
        DigitalTwin.update(com.bhoomibot.os.feature.autonomous.simulation.engine.RobotStateVector())
        FarmWorld.clearWorld()
        simEngine.start()

        // --- TEST CASE 1: CONVERSATION & KNOWLEDGE ---
        println("[STEP 1] Testing Natural Language Understanding...")
        val msg = "Carry fertilizer to Tomato Field"
        val response = agent.processIntent(msg)
        
        // Verify Gap Detection
        assertTrue("Robot should ask for Tomato Field location", response is AgentResponse.ClarificationNeeded)
        println("Result: Success - Robot identified unknown location.")

        // --- TEST CASE 2: TEACHING & MEMORY ---
        println("[STEP 2] Teaching robot 'Tomato Field' location...")
        knowledge.storeNode(KnowledgeNode(
            name = "TOMATO FIELD",
            type = NodeType.ZONE,
            metadata = mapOf("lat" to "12.97", "lon" to "77.59")
        ))
        val secondResponse = agent.processIntent(msg)
        assertTrue("Plan should be ready after teaching", secondResponse is AgentResponse.PlanReady)
        println("Result: Success - Robot stored location in Knowledge Graph.")

        // --- TEST CASE 3: PHYSICAL EXECUTION & PERCEPTION ---
        println("[STEP 3] Executing Skill: Navigation & Perception...")
        // Simulate Robot Moving Forward
        robotRepo.updateSpeed(80)
        robotRepo.sendDriveCommand(com.bhoomibot.os.model.DriveCommand.FORWARD)
        
        // Spawn an obstacle 3m ahead
        FarmWorld.spawnObstacle(x = 3.0, y = 0.0, label = "ROCK")
        
        delay(500) // Wait for sim
        val obs = perception.observationStream.first()
        
        // Verify Perception
        assertTrue("Perception should see the ROCK", obs.any { it.label == "ROCK" })
        println("Result: Success - Virtual Robot 'sees' the obstacle in world model.")

        // --- TEST CASE 4: DIGITAL TWIN TRACKING ---
        delay(1000)
        val twin = DigitalTwin.state.value
        assertTrue("Robot should have physically moved in Digital Twin", twin.x > 0)
        println("Result: Success - Digital Twin synced at position: (${twin.x}, ${twin.y})")

        simEngine.stop()
        println("=== FULL SYSTEM INTEGRATION TEST PASSED ===")
    }
}
