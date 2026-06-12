package com.oceanofmaya.intervalwalktrainer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WorkoutSessionMetricsMergerTest {
    @Test
    fun `refresh prefers higher synced step count`() {
        val session = WorkoutSession(
            date = "2026-06-07",
            workoutType = "3-3 Japanese - 5 Rounds",
            minutes = 30,
            stepCount = 21,
            averageHeartRateBpm = 96,
            minHeartRateBpm = 96,
            maxHeartRateBpm = 96
        )
        val metrics = WorkoutMetricsSummary(
            stepCount = 3210,
            averageHeartRateBpm = 97,
            minHeartRateBpm = 80,
            maxHeartRateBpm = 112,
            fastPhaseAverageHeartRateBpm = 112,
            slowPhaseAverageHeartRateBpm = 97
        )

        val refreshed = WorkoutSessionMetricsMerger.refresh(session, metrics)

        assertEquals(3210, refreshed.stepCount)
        assertEquals(97, refreshed.averageHeartRateBpm)
        assertEquals(80, refreshed.minHeartRateBpm)
        assertEquals(112, refreshed.maxHeartRateBpm)
        assertEquals(112, refreshed.fastPhaseAverageHeartRateBpm)
        assertEquals(97, refreshed.slowPhaseAverageHeartRateBpm)
    }
}
