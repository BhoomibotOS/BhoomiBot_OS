package com.bhoomibot.ai

import com.bhoomibot.ai.agent.*
import com.bhoomibot.ai.planner.TaskPlanner
import com.bhoomibot.sdk.*

/**
 * The Master Brain of BhoomiBot.
 * This is the high-level conductor that can run on any platform.
 */
class MasterBrain(
    private val agentDriver: AgentDriver? = null
) : BrainAgent {

    private val fallbackParser = DialogueManager()
    private val planner = TaskPlanner()

    override suspend fun processCommand(text: String): BrainResponse {
        // 1. Try High-Intelligence Parser (Gemma/Llama)
        val intent = agentDriver?.parseIntent(text, emptyList()) 
            ?: fallbackParser.parse(text) // 2. Fallback to basic rules
            ?: return BrainResponse.Feedback("I'm sorry, I don't understand that command.")

        // 3. Handle simple feedback actions
        if (intent.action == RobotAction.STATUS) {
            return BrainResponse.Feedback("System Status: All layers operational. Ready for commands.")
        }
        
        if (intent.action == RobotAction.HELP) {
            return BrainResponse.Feedback(
                "I am your BhoomiBot Assistant. You can tell me to:\n" +
                "• 'Go to [Location]' - Navigate the robot.\n" +
                "• 'Carry [Item] from [Start] to [End]' - Transport objects.\n" +
                "• 'Status' - Check system health.\n" +
                "• 'Teach [Skill]' - Start manual training mode."
            )
        }

        // 4. Generate Strategic Plan
        val taskPlan = planner.generatePlan(intent)
        
        return if (taskPlan.steps.isNotEmpty()) {
            BrainResponse.Plan(taskPlan.steps)
        } else {
            BrainResponse.Feedback("Intent recognized, but no physical plan could be generated.")
        }
    }
}
