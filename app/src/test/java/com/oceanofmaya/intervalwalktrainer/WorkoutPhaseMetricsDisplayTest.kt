package com.oceanofmaya.intervalwalktrainer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WorkoutPhaseMetricsDisplayTest {
    @Test
    fun `formatPhaseHeartRate uses pending label when workout heart rate exists`() {
        val display = WorkoutPhaseMetricsDisplay.formatPhaseHeartRate(
            heartRateBpm = null,
            hasWorkoutHeartRate = true,
            anyPhaseHeartRateResolved = false,
            unavailableLabel = "—",
            pendingLabel = "Pending sync",
            formatBpm = { "$it bpm" }
        )

        assertTrue(display.isPlaceholder)
        assertEquals("Pending sync", display.text)
    }

    @Test
    fun `formatPhaseHeartRate uses unavailable when peer phase already resolved`() {
        val display = WorkoutPhaseMetricsDisplay.formatPhaseHeartRate(
            heartRateBpm = null,
            hasWorkoutHeartRate = true,
            anyPhaseHeartRateResolved = true,
            unavailableLabel = "—",
            pendingLabel = "Pending sync",
            formatBpm = { "$it bpm" }
        )

        assertTrue(display.isPlaceholder)
        assertEquals("—", display.text)
    }

    @Test
    fun `formatPhaseHeartRate uses unavailable label when no workout heart rate`() {
        val display = WorkoutPhaseMetricsDisplay.formatPhaseHeartRate(
            heartRateBpm = null,
            hasWorkoutHeartRate = false,
            anyPhaseHeartRateResolved = false,
            unavailableLabel = "—",
            pendingLabel = "Pending sync",
            formatBpm = { "$it bpm" }
        )

        assertTrue(display.isPlaceholder)
        assertEquals("—", display.text)
    }

    @Test
    fun `shouldShowPhaseMetrics when formula snapshot exists`() {
        val session = WorkoutSession(
            date = "2026-06-07",
            workoutType = "3-3 Japanese - 5 Rounds",
            minutes = 30,
            formulaSnapshotJson = "180,180,5,0,0"
        )

        assertTrue(WorkoutPhaseMetricsDisplay.shouldShowPhaseMetrics(session))
    }

    @Test
    fun `shouldShowPhaseMetrics is false without context or values`() {
        val session = WorkoutSession(
            date = "2026-06-07",
            workoutType = "3-3 Japanese - 5 Rounds",
            minutes = 30
        )

        assertFalse(WorkoutPhaseMetricsDisplay.shouldShowPhaseMetrics(session))
    }

    @Test
    fun `shouldShowMetricPlaceholders requires metrics enabled and phase context`() {
        val session = WorkoutSession(
            date = "2026-06-07",
            workoutType = "3-3 Japanese - 5 Rounds",
            minutes = 30,
            formulaSnapshotJson = "180,180,5,0,0"
        )

        assertTrue(WorkoutPhaseMetricsDisplay.shouldShowMetricPlaceholders(metricsEnabled = true, session = session))
        assertFalse(WorkoutPhaseMetricsDisplay.shouldShowMetricPlaceholders(metricsEnabled = false, session = session))
    }
}
