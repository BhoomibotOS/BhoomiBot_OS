package com.bhoomibot.ai.perception

import com.bhoomibot.sdk.Observation

/**
 * L4 Perception Layer Interface.
 * AI modules should produce standardized Observations.
 */
interface PerceptionProvider {
    fun getCurrentObservations(): List<Observation>
}
