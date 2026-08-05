package com.bhoomibot.os.feature.autonomous.planning

import com.bhoomibot.os.feature.autonomous.world.WorldModel
import com.bhoomibot.os.model.DriveCommand
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * MotionPlanner: Decides the next movement action.
 */
object MotionPlanner {

    private val _nextCommand = MutableStateFlow(DriveCommand.STOP)
    val nextCommand = _nextCommand.asStateFlow()

    private val _targetSpeed = MutableStateFlow(0)
    val targetSpeed = _targetSpeed.asStateFlow()

    /**
     * compute: The main strategy loop.
     * Takes current position and objects from WorldModel and decides the next move.
     */
    fun compute() {
        val currentPos = WorldModel.robotPose.value
        val map = WorldModel.semanticMap.value
        
        // Strategy:
        // 1. If mission target reached -> STOP.
        // 2. If obstacle in way -> DEVIATE.
        // 3. If weed detected -> SLOW DOWN.
    }
}
