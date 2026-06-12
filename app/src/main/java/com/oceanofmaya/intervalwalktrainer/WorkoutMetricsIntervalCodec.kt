package com.oceanofmaya.intervalwalktrainer

object WorkoutMetricsIntervalCodec {
    private const val INTERVAL_SEPARATOR = ";"
    private const val BOUND_SEPARATOR = ","
    private const val MILLIS_PER_MINUTE = 60_000L
    private const val COVERAGE_THRESHOLD = 0.75

    fun encode(intervals: List<WorkoutMetricsInterval>): String? {
        if (intervals.isEmpty()) return null
        return intervals.joinToString(INTERVAL_SEPARATOR) { interval ->
            "${interval.startedAtMillis}$BOUND_SEPARATOR${interval.endedAtMillis}"
        }
    }

    fun decode(encoded: String?): List<WorkoutMetricsInterval> {
        if (encoded.isNullOrBlank()) return emptyList()
        return encoded.split(INTERVAL_SEPARATOR).mapNotNull { part ->
            val bounds = part.split(BOUND_SEPARATOR)
            if (bounds.size != 2) return@mapNotNull null
            val start = bounds[0].toLongOrNull() ?: return@mapNotNull null
            val end = bounds[1].toLongOrNull() ?: return@mapNotNull null
            if (end <= start) return@mapNotNull null
            WorkoutMetricsInterval(start, end)
        }
    }

    fun resolveIntervals(session: WorkoutSession): List<WorkoutMetricsInterval> {
        return resolveTrackedIntervals(session)
    }

    fun resolveTrackedIntervals(session: WorkoutSession): List<WorkoutMetricsInterval> {
        val storedIntervals = decode(session.metricsIntervalsJson)
        if (storedIntervals.isNotEmpty()) {
            return storedIntervals
        }
        val startedAt = session.startedAt ?: estimateStartedAt(session)
        val endedAt = session.timestamp
        return if (endedAt > startedAt) {
            listOf(WorkoutMetricsInterval(startedAt, endedAt))
        } else {
            emptyList()
        }
    }

    fun resolveReadIntervals(session: WorkoutSession): List<WorkoutMetricsInterval> {
        return resolveReadIntervals(
            trackedIntervals = decode(session.metricsIntervalsJson),
            sessionStartedAtMillis = session.startedAt,
            completedAtMillis = session.timestamp,
            expectedDurationMillis = session.minutes * MILLIS_PER_MINUTE
        )
    }

    fun resolveReadIntervals(
        trackedIntervals: List<WorkoutMetricsInterval>,
        sessionStartedAtMillis: Long?,
        completedAtMillis: Long,
        expectedDurationMillis: Long
    ): List<WorkoutMetricsInterval> {
        val estimatedStart = canonicalSessionStartedAt(
            trackedStartedAtMillis = sessionStartedAtMillis,
            completedAtMillis = completedAtMillis,
            expectedDurationMillis = expectedDurationMillis
        )
        val readInterval = when {
            completedAtMillis <= estimatedStart -> null
            trackedIntervals.isEmpty() ->
                WorkoutMetricsInterval(estimatedStart, completedAtMillis)
            trackedIntervals.sumOf { it.durationMillis } < expectedDurationMillis * COVERAGE_THRESHOLD ->
                WorkoutMetricsInterval(
                    startedAtMillis = minOf(estimatedStart, trackedIntervals.minOf { it.startedAtMillis }),
                    endedAtMillis = maxOf(
                        completedAtMillis,
                        trackedIntervals.maxOf { it.endedAtMillis }
                    )
                )
            else -> WorkoutMetricsInterval(
                trackedIntervals.minOf { it.startedAtMillis },
                trackedIntervals.maxOf { it.endedAtMillis }
            )
        }
        return readInterval?.let { listOf(it) } ?: emptyList()
    }

    fun resolvePhaseMappingIntervals(session: WorkoutSession): List<WorkoutMetricsInterval> {
        val trackedIntervals = decode(session.metricsIntervalsJson)
        val expectedDurationMillis = session.minutes * MILLIS_PER_MINUTE
        if (trackedIntervals.isNotEmpty() &&
            trackedIntervals.sumOf { it.durationMillis } >= expectedDurationMillis * COVERAGE_THRESHOLD
        ) {
            return trackedIntervals.sortedBy { it.startedAtMillis }
        }
        return resolveReadIntervals(session)
    }

    fun estimateStartedAt(session: WorkoutSession): Long {
        return (session.timestamp - session.minutes * MILLIS_PER_MINUTE).coerceAtLeast(0L)
    }

    fun canonicalSessionStartedAt(
        trackedStartedAtMillis: Long?,
        completedAtMillis: Long,
        expectedDurationMillis: Long
    ): Long {
        val estimatedStart = (completedAtMillis - expectedDurationMillis).coerceAtLeast(0L)
        return when (trackedStartedAtMillis) {
            null -> estimatedStart
            else -> minOf(trackedStartedAtMillis, estimatedStart)
        }
    }
}
