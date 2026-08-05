package com.bhoomibot.os.feature.autonomous.simulation

import com.bhoomibot.os.feature.autonomous.agent.AgentLayerImpl
import com.bhoomibot.os.feature.autonomous.core.interfaces.AgentResponse
import com.bhoomibot.os.feature.autonomous.core.model.KnowledgeNode
import com.bhoomibot.os.feature.autonomous.core.model.NodeType
import com.bhoomibot.os.feature.autonomous.knowledge.KnowledgeLayerImpl
import com.bhoomibot.os.feature.autonomous.planning.PlannerLayerImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * AI REASONING VALIDATOR: Tests the "ChatGPT for Robots" multi-turn logic.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33]) // Standard stable SDK for Robolectric
class AgentReasoningTest {

    @Test
    fun testUnknownLocationReasoning() = runBlocking {
        // 1. Setup Cognitive Stack with an Empty Knowledge Graph
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        val knowledge = KnowledgeLayerImpl(context)
        val planner = PlannerLayerImpl()
        val agent = AgentLayerImpl(knowledge, planner)

        // 2. User gives an incomplete command
        val firstCommand = "Go to the Mango Patch"
        val firstResponse = agent.processIntent(firstCommand)

        // 3. VALIDATE: Agent should identify that "Mango Patch" is unknown
        assertTrue("Should request clarification, but got: $firstResponse", firstResponse is AgentResponse.ClarificationNeeded)
        val clarification = firstResponse as AgentResponse.ClarificationNeeded
        assertTrue(clarification.query.contains("Mango Patch"))
        
        // 4. Simulate User Teaching (Adding Knowledge to L1)
        knowledge.storeNode(KnowledgeNode(
            name = "MANGO PATCH",
            type = NodeType.ZONE,
            metadata = mapOf("lat" to "12.9", "lon" to "77.5")
        ))

        // 5. User repeats the command
        val secondResponse = agent.processIntent(firstCommand)

        // 6. VALIDATE: Now the Agent should succeed and create a Plan (L2)
        assertTrue("Should now have a ready plan", secondResponse is AgentResponse.PlanReady)
        val planResponse = secondResponse as AgentResponse.PlanReady
        assertEquals("Should have steps in the plan", 1, planResponse.plan.steps.size)
        assertEquals("NAVIGATE", planResponse.plan.steps[0].skillId)
    }

    @Test
    fun testComplexTransportPlanning() = runBlocking {
        val context = org.robolectric.RuntimeEnvironment.getApplication()
        val knowledge = KnowledgeLayerImpl(context)
        val planner = PlannerLayerImpl()
        val agent = AgentLayerImpl(knowledge, planner)
        
        // Define destination first
        knowledge.storeNode(KnowledgeNode(name = "FIELD B", type = NodeType.ZONE))

        // User command for transport
        val command = "Carry load to Field B"
        val response = agent.processIntent(command)

        // VALIDATE: The Planner (L2) should decompose this into 4 steps
        assertTrue(response is AgentResponse.PlanReady)
        val plan = (response as AgentResponse.PlanReady).plan
        
        // Transport skill sequence: NAV(Shed) -> ATTACH -> NAV(Dest) -> DETACH
        assertEquals("Should have 4 steps for transport", 4, plan.steps.size)
        assertEquals("NAVIGATE", plan.steps[0].skillId)
        assertEquals("ATTACH", plan.steps[1].skillId)
        assertEquals("NAVIGATE", plan.steps[2].skillId)
        assertEquals("DETACH", plan.steps[3].skillId)
    }
}
