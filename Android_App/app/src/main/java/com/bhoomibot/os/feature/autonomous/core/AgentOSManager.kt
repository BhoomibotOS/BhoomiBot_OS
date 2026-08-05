package com.bhoomibot.os.feature.autonomous.core

import android.app.Application
import android.content.Context
import com.bhoomibot.os.feature.autonomous.agent.AgentLayerImpl
import com.bhoomibot.os.feature.autonomous.ai.PerceptionLayerImpl
import com.bhoomibot.os.feature.autonomous.control.ControlLayerImpl
import com.bhoomibot.os.feature.autonomous.core.interfaces.*
import com.bhoomibot.os.feature.autonomous.hardware.HardwareLayerImpl
import com.bhoomibot.os.feature.autonomous.knowledge.KnowledgeLayerImpl
import com.bhoomibot.os.feature.autonomous.localization.LocalizationLayerImpl
import com.bhoomibot.os.feature.autonomous.planning.PlannerLayerImpl
import com.bhoomibot.os.feature.autonomous.skills.SkillLayerImpl
import com.bhoomibot.os.feature.autonomous.world.WorldModelLayerImpl
import com.bhoomibot.os.repository.provideRobotRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * AgentOSManager: The central conductor of the 9-layer stack.
 * 
 * JUNIOR ENGINEER NOTE: This is where we "plug in" all the layers. 
 * It manages the flow of data from the human (L0) down to the wheels (L8).
 */
class AgentOSManager(context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Plug in implementations
    val knowledge: KnowledgeLayer = KnowledgeLayerImpl(context)
    val planner: PlannerLayer = PlannerLayerImpl()
    val agent: AgentLayer = AgentLayerImpl(context, knowledge, planner)
    val skills: SkillLayer = SkillLayerImpl()
    val perception: PerceptionLayer = PerceptionLayerImpl(context)
    val localization: LocalizationLayer = LocalizationLayerImpl(context)
    val worldModel: WorldModelLayer = WorldModelLayerImpl()
    val control: ControlLayer = ControlLayerImpl()
    val hardware: HardwareLayer = HardwareLayerImpl(provideRobotRepository(context as android.app.Application))

    /**
     * Start the autonomous background loop.
     */
    fun startCoreLoop() {
        scope.launch {
            localization.getRobotPose().collect { pose ->
                // 1. Sync perception and pose into the World Model
                // (In a real system, this would be high-frequency)
                worldModel.syncPerception(pose, emptyList())
            }
        }
    }

    /**
     * Execute a specific user command.
     */
    suspend fun executeNaturalCommand(text: String): AgentResponse {
        return agent.processIntent(text)
    }
}
