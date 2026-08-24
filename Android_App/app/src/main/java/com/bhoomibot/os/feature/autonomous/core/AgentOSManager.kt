package com.bhoomibot.os.feature.autonomous.core

import android.content.Context
import com.bhoomibot.ai.MasterBrain
import com.bhoomibot.ai.agent.AgentDriver
import com.bhoomibot.ai.agent.CloudAgentDriver
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
 */
class AgentOSManager(context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // AI-Fix: API Key for Llama-3 Cloud Wisdom
    private val cloudDriver: AgentDriver = CloudAgentDriver("YOUR_API_KEY_HERE")
    val masterBrain = MasterBrain(cloudDriver)

    // Local Android Layers
    val knowledge: KnowledgeLayer = KnowledgeLayerImpl(context)
    val planner: PlannerLayer = PlannerLayerImpl()
    val agent: AgentLayer = AgentLayerImpl(context, knowledge, planner, masterBrain)
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
                worldModel.syncPerception(pose, emptyList())
            }
        }
    }

    /**
     * Execute a user command.
     */
    suspend fun executeNaturalCommand(text: String): AgentResponse {
        return agent.processIntent(text)
    }
}
