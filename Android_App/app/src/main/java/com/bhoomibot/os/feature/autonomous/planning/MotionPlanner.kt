package com.bhoomibot.os.feature.autonomous.planning

import com.bhoomibot.sdk.*

/**
 * High-level Motion Planner Proxy.
 */
class MotionPlanner {
    fun createPlan(intent: RobotIntent): TaskPlan {
        return TaskPlan(emptyList())
    }
}
