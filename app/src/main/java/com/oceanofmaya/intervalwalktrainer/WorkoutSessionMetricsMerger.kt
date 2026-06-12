package com.oceanofmaya.intervalwalktrainer

object WorkoutSessionMetricsMerger {
    fun merge(session: WorkoutSession, metrics: WorkoutMetricsSummary): WorkoutSession {
        return session.copy(
            stepCount = session.stepCount ?: metrics.stepCount,
            averageHeartRateBpm = session.averageHeartRateBpm ?: metrics.averageHeartRateBpm,
            minHeartRateBpm = session.minHeartRateBpm ?: metrics.minHeartRateBpm,
            maxHeartRateBpm = session.maxHeartRateBpm ?: metrics.maxHeartRateBpm,
            stepSource = session.stepSource ?: metrics.stepSource,
            heartRateSource = session.heartRateSource ?: metrics.heartRateSource,
            startedAt = session.startedAt ?: metrics.startedAt,
            metricsIntervalsJson = session.metricsIntervalsJson
                ?: WorkoutMetricsIntervalCodec.encode(metrics.intervals).takeIf { metrics.intervals.isNotEmpty() },
            formulaSnapshotJson = session.formulaSnapshotJson
                ?: metrics.formulaSnapshot?.let { WorkoutMetricsCodec.encodeFormulaSnapshot(it) },
            metricsPhaseWindowsJson = mergedPhaseWindowsJson(session, metrics),
            fastPhaseAverageHeartRateBpm = session.fastPhaseAverageHeartRateBpm ?: metrics.fastPhaseAverageHeartRateBpm,
            slowPhaseAverageHeartRateBpm = session.slowPhaseAverageHeartRateBpm ?: metrics.slowPhaseAverageHeartRateBpm
        )
    }

    fun refresh(session: WorkoutSession, metrics: WorkoutMetricsSummary): WorkoutSession {
        val merged = merge(session, metrics)
        val canonicalStartedAt = WorkoutMetricsIntervalCodec.canonicalSessionStartedAt(
            trackedStartedAtMillis = merged.startedAt,
            completedAtMillis = session.timestamp,
            expectedDurationMillis = session.minutes * 60_000L
        )
        return merged.copy(
            startedAt = canonicalStartedAt,
            stepCount = preferHigherStepCount(session.stepCount, metrics.stepCount),
            averageHeartRateBpm = metrics.averageHeartRateBpm ?: merged.averageHeartRateBpm,
            minHeartRateBpm = preferLowerHeartRate(session.minHeartRateBpm, metrics.minHeartRateBpm),
            maxHeartRateBpm = preferHigherHeartRate(session.maxHeartRateBpm, metrics.maxHeartRateBpm),
            fastPhaseAverageHeartRateBpm = metrics.fastPhaseAverageHeartRateBpm ?: merged.fastPhaseAverageHeartRateBpm,
            slowPhaseAverageHeartRateBpm = metrics.slowPhaseAverageHeartRateBpm ?: merged.slowPhaseAverageHeartRateBpm
        )
    }

    private fun preferHigherStepCount(existing: Int?, refreshed: Int?): Int? {
        return when {
            existing == null -> refreshed
            refreshed == null -> existing
            else -> maxOf(existing, refreshed)
        }
    }

    private fun preferLowerHeartRate(existing: Int?, refreshed: Int?): Int? {
        return when {
            existing == null -> refreshed
            refreshed == null -> existing
            else -> minOf(existing, refreshed)
        }
    }

    private fun preferHigherHeartRate(existing: Int?, refreshed: Int?): Int? {
        return when {
            existing == null -> refreshed
            refreshed == null -> existing
            else -> maxOf(existing, refreshed)
        }
    }

    private fun mergedPhaseWindowsJson(
        session: WorkoutSession,
        metrics: WorkoutMetricsSummary
    ): String? {
        if (metrics.phaseWindows.isEmpty()) {
            return session.metricsPhaseWindowsJson
        }
        return WorkoutMetricsCodec.encodePhaseWindows(metrics.phaseWindows)
    }
}
