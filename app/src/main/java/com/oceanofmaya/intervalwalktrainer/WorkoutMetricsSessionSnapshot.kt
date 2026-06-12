package com.oceanofmaya.intervalwalktrainer

data class WorkoutMetricsSessionSnapshot(
    val sessionStartedAtMillis: Long,
    val activeIntervals: List<WorkoutMetricsInterval>,
    val activeIntervalStartedAtMillis: Long?,
    val running: Boolean
)
