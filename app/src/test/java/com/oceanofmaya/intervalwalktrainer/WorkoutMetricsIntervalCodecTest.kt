package com.oceanofmaya.intervalwalktrainer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WorkoutMetricsIntervalCodecTest {
    @Test
    fun `encode and decode round trip intervals`() {
        val intervals = listOf(
            WorkoutMetricsInterval(1_000L, 2_000L),
            WorkoutMetricsInterval(5_000L, 8_000L)
        )

        val encoded = WorkoutMetricsIntervalCodec.encode(intervals)

        assertEquals(intervals, WorkoutMetricsIntervalCodec.decode(encoded))
    }

    @Test
    fun `resolveIntervals prefers stored intervals`() {
        val session = WorkoutSession(
            date = "2026-06-07",
            workoutType = "3-3 Japanese - 5 Rounds",
            minutes = 30,
            timestamp = 20_000L,
            startedAt = 1_000L,
            metricsIntervalsJson = "1000,5000;7000,20000"
        )

        assertEquals(
            listOf(
                WorkoutMetricsInterval(1_000L, 5_000L),
                WorkoutMetricsInterval(7_000L, 20_000L)
            ),
            WorkoutMetricsIntervalCodec.resolveIntervals(session)
        )
    }

    @Test
    fun `resolveIntervals falls back to startedAt window`() {
        val session = WorkoutSession(
            date = "2026-06-07",
            workoutType = "3-3 Japanese - 5 Rounds",
            minutes = 30,
            timestamp = 3_600_000L,
            startedAt = 1_800_000L
        )

        assertEquals(
            listOf(WorkoutMetricsInterval(1_800_000L, 3_600_000L)),
            WorkoutMetricsIntervalCodec.resolveIntervals(session)
        )
    }

    @Test
    fun `resolveIntervals estimates start when startedAt missing`() {
        val session = WorkoutSession(
            date = "2026-06-07",
            workoutType = "3-3 Japanese - 5 Rounds",
            minutes = 10,
            timestamp = 600_000L
        )

        val intervals = WorkoutMetricsIntervalCodec.resolveIntervals(session)

        assertEquals(1, intervals.size)
        assertEquals(0L, intervals.first().startedAtMillis)
        assertEquals(600_000L, intervals.first().endedAtMillis)
    }

    @Test
    fun `decode ignores invalid intervals`() {
        assertTrue(WorkoutMetricsIntervalCodec.decode("1000,2000;bad;3000,4000").size == 2)
    }

    @Test
    fun `resolveReadIntervals ignores late startedAt when expanding short tracked coverage`() {
        val session = WorkoutSession(
            date = "2026-06-07",
            workoutType = "3-3 Japanese - 5 Rounds",
            minutes = 30,
            timestamp = 3_600_000L,
            startedAt = 3_599_000L,
            metricsIntervalsJson = "3599000,3600000"
        )

        assertEquals(
            listOf(WorkoutMetricsInterval(1_800_000L, 3_600_000L)),
            WorkoutMetricsIntervalCodec.resolveReadIntervals(session)
        )
    }

    @Test
    fun `resolveReadIntervals expands short tracked coverage to full workout window`() {
        val session = WorkoutSession(
            date = "2026-06-07",
            workoutType = "3-3 Japanese - 5 Rounds",
            minutes = 30,
            timestamp = 3_600_000L,
            startedAt = 1_800_000L,
            metricsIntervalsJson = "3599000,3600000"
        )

        assertEquals(
            listOf(WorkoutMetricsInterval(1_800_000L, 3_600_000L)),
            WorkoutMetricsIntervalCodec.resolveReadIntervals(session)
        )
    }

    @Test
    fun `resolveReadIntervals consolidates well tracked workout into one window`() {
        val session = WorkoutSession(
            date = "2026-06-07",
            workoutType = "3-3 Japanese - 5 Rounds",
            minutes = 30,
            timestamp = 3_600_000L,
            startedAt = 1_800_000L,
            metricsIntervalsJson = "1800000,2700000;2800000,3600000"
        )

        assertEquals(
            listOf(WorkoutMetricsInterval(1_800_000L, 3_600_000L)),
            WorkoutMetricsIntervalCodec.resolveReadIntervals(session)
        )
    }

    @Test
    fun `canonicalSessionStartedAt prefers earlier estimated workout start`() {
        assertEquals(
            1_800_000L,
            WorkoutMetricsIntervalCodec.canonicalSessionStartedAt(
                trackedStartedAtMillis = 3_500_000L,
                completedAtMillis = 3_600_000L,
                expectedDurationMillis = 1_800_000L
            )
        )
    }
}
