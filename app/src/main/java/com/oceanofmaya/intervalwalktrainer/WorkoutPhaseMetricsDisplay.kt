package com.oceanofmaya.intervalwalktrainer

data class WorkoutPhaseHeartRateDisplay(
    val text: String,
    val isPlaceholder: Boolean
)

object WorkoutPhaseMetricsDisplay {
    fun shouldShowPhaseMetrics(session: WorkoutSession): Boolean {
        return session.hasPhaseMetricsContext() ||
            session.fastPhaseAverageHeartRateBpm != null ||
            session.slowPhaseAverageHeartRateBpm != null
    }

    fun shouldShowPhaseMetrics(sessions: List<WorkoutSession>): Boolean {
        return sessions.any { shouldShowPhaseMetrics(it) }
    }

    fun shouldShowMetricPlaceholders(metricsEnabled: Boolean, session: WorkoutSession): Boolean {
        return metricsEnabled && shouldShowPhaseMetrics(session)
    }

    fun shouldShowMetricPlaceholders(metricsEnabled: Boolean, sessions: List<WorkoutSession>): Boolean {
        return metricsEnabled && shouldShowPhaseMetrics(sessions)
    }

    fun formatPhaseHeartRate(
        heartRateBpm: Int?,
        hasWorkoutHeartRate: Boolean,
        anyPhaseHeartRateResolved: Boolean,
        unavailableLabel: String,
        pendingLabel: String,
        formatBpm: (Int) -> String
    ): WorkoutPhaseHeartRateDisplay {
        return when {
            heartRateBpm != null -> WorkoutPhaseHeartRateDisplay(
                text = formatBpm(heartRateBpm),
                isPlaceholder = false
            )
            hasWorkoutHeartRate && !anyPhaseHeartRateResolved -> WorkoutPhaseHeartRateDisplay(
                text = pendingLabel,
                isPlaceholder = true
            )
            else -> WorkoutPhaseHeartRateDisplay(
                text = unavailableLabel,
                isPlaceholder = true
            )
        }
    }
}
