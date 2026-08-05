package com.bhoomibot.os.feature.autonomous.simulation.world

import android.graphics.RectF
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * FARM WORLD: The virtual environment repository.
 * 
 * Stores all static and dynamic entities in the simulation (Rocks, Humans, Crops).
 */
object FarmWorld {

    private val _entities = MutableStateFlow<List<VirtualEntity>>(emptyList())
    val entities = _entities.asStateFlow()

    /**
     * Spawns an object into the virtual world.
     */
    fun spawnObstacle(x: Double, y: Double, label: String, radius: Float = 0.5f) {
        val newEntity = VirtualEntity(
            id = "obs_${System.currentTimeMillis()}",
            label = label,
            x = x,
            y = y,
            radiusMeters = radius
        )
        _entities.value += newEntity
    }

    fun clearWorld() {
        _entities.value = emptyList()
    }
}

/**
 * A physical object in the virtual farm.
 */
data class VirtualEntity(
    val id: String,
    val label: String,
    val x: Double,
    val y: Double,
    val radiusMeters: Float
)
