package com.bhoomibot.os.feature.autonomous.ai

import android.content.Context
import android.graphics.Bitmap
import com.bhoomibot.os.feature.autonomous.core.interfaces.PerceptionLayer
import com.bhoomibot.sdk.Observation
import com.bhoomibot.sdk.BoundingBox

/**
 * FEATURE: L4 Perception Layer Implementation
 * AI-Fix: Maps local engine detections to Standalone SDK Observations
 */
class PerceptionLayerImpl(context: Context) : PerceptionLayer {

    private val engine = PerceptionEngine(context)

    override suspend fun analyzeFrame(bitmap: Bitmap): List<Observation> {
        engine.analyzeFrame(bitmap)
        
        return engine.detectedWeeds.value.map { det ->
            Observation(
                label = det.label,
                confidence = det.confidence,
                box = BoundingBox(
                    left = det.boundingBox.left,
                    top = det.boundingBox.top,
                    right = det.boundingBox.right,
                    bottom = det.boundingBox.bottom
                ),
                timestamp = System.currentTimeMillis()
            )
        }
    }
}
