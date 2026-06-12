package com.oceanofmaya.intervalwalktrainer

/**
 * Persisted per-session metric summary. Null fields mean the provider was unavailable,
 * unpermitted, or returned no useful samples for the completed workout window.
 */
data class WorkoutMetricsSummary(
    val stepCount: Int? = null,
    val averageHeartRateBpm: Int? = null,
    val minHeartRateBpm: Int? = null,
    val maxHeartRateBpm: Int? = null,
    val stepSource: String? = null,
    val heartRateSource: String? = null,
    val startedAt: Long? = null,
    val intervals: List<WorkoutMetricsInterval> = emptyList(),
    val formulaSnapshot: WorkoutFormulaSnapshot? = null,
    val phaseWindows: List<WorkoutPhaseWindow> = emptyList(),
    val fastPhaseAverageHeartRateBpm: Int? = null,
    val slowPhaseAverageHeartRateBpm: Int? = null
) {
    val hasDisplayableValue: Boolean
        get() = stepCount != null ||
            averageHeartRateBpm != null ||
            fastPhaseAverageHeartRateBpm != null ||
            slowPhaseAverageHeartRateBpm != null
}

data class WorkoutMetricsInterval(
    val startedAtMillis: Long,
    val endedAtMillis: Long
) {
    val durationMillis: Long
        get() = (endedAtMillis - startedAtMillis).coerceAtLeast(0L)
}

object WorkoutMetricsSources {
    const val HEALTH_CONNECT = "health_connect"
}
