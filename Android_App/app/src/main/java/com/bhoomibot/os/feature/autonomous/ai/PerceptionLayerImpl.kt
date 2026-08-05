package com.bhoomibot.os.feature.autonomous.ai

import android.content.Context
import android.graphics.Bitmap
import com.bhoomibot.os.feature.autonomous.core.interfaces.PerceptionLayer
import com.bhoomibot.os.feature.autonomous.core.model.Observation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * FEATURE: L4 Perception Layer Implementation
 * 
 * JUNIOR ENGINEER NOTE: This layer uses AI (YOLO/TFLite) to convert
 * pixels into robot-readable observations.
 */
class PerceptionLayerImpl(context: Context) : PerceptionLayer {

    private val engine = PerceptionEngine(context)

    override suspend fun analyzeFrame(bitmap: Bitmap): List<Observation> {
        engine.analyzeFrame(bitmap)
        
        // Map engine's internal detections to the standard L4 Contract
        return engine.detectedWeeds.value.map { det ->
            Observation(
                label = det.label,
                confidence = det.confidence,
                boundingBox = det.boundingBox,
                timestamp = System.currentTimeMillis()
            )
        }
    }
}
