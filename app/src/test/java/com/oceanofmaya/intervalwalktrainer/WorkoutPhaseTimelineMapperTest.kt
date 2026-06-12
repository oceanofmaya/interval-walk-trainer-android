package com.oceanofmaya.intervalwalktrainer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WorkoutPhaseTimelineMapperTest {
    @Test
    fun `maps both slow and fast phases for a full slow-start workout`() {
        val phases = WorkoutPhaseSequenceBuilder.build(IntervalFormulas.formula2)
        val activeIntervals = listOf(
            WorkoutMetricsInterval(
                startedAtMillis = 0L,
                endedAtMillis = IntervalFormulas.formula2.totalDurationSeconds * 1000L
            )
        )

        val windows = WorkoutPhaseTimelineMapper.map(phases, activeIntervals)

        assertTrue(windows.any { it.phase == WorkoutPhaseType.SLOW })
        assertTrue(windows.any { it.phase == WorkoutPhaseType.FAST })
    }

    @Test
    fun `advances phases with sub-second interval slices`() {
        val phases = listOf(
            WorkoutPhaseSegment(WorkoutPhaseType.SLOW, 2),
            WorkoutPhaseSegment(WorkoutPhaseType.FAST, 2)
        )
        val activeIntervals = listOf(
            WorkoutMetricsInterval(0L, 500L),
            WorkoutMetricsInterval(500L, 1_000L),
            WorkoutMetricsInterval(1_000L, 1_500L),
            WorkoutMetricsInterval(1_500L, 2_000L),
            WorkoutMetricsInterval(2_000L, 2_500L),
            WorkoutMetricsInterval(2_500L, 3_000L),
            WorkoutMetricsInterval(3_000L, 3_500L),
            WorkoutMetricsInterval(3_500L, 4_000L)
        )

        val windows = WorkoutPhaseTimelineMapper.map(phases, activeIntervals)

        assertEquals(2, windows.size)
        assertEquals(WorkoutPhaseType.SLOW, windows[0].phase)
        assertEquals(WorkoutPhaseType.FAST, windows[1].phase)
    }

    @Test
    fun `maps phases across paused active intervals`() {
        val phases = listOf(
            WorkoutPhaseSegment(WorkoutPhaseType.SLOW, 120),
            WorkoutPhaseSegment(WorkoutPhaseType.FAST, 120)
        )
        val activeIntervals = listOf(
            WorkoutMetricsInterval(1_000L, 70_000L),
            WorkoutMetricsInterval(130_000L, 250_000L)
        )

        val windows = WorkoutPhaseTimelineMapper.map(phases, activeIntervals)

        assertEquals(3, windows.size)
        assertEquals(WorkoutPhaseType.SLOW, windows[0].phase)
        assertEquals(1_000L, windows[0].startedAtMillis)
        assertEquals(70_000L, windows[0].endedAtMillis)
        assertEquals(WorkoutPhaseType.SLOW, windows[1].phase)
        assertEquals(130_000L, windows[1].startedAtMillis)
        assertEquals(181_000L, windows[1].endedAtMillis)
        assertEquals(WorkoutPhaseType.FAST, windows[2].phase)
        assertEquals(181_000L, windows[2].startedAtMillis)
        assertEquals(250_000L, windows[2].endedAtMillis)
    }
}
