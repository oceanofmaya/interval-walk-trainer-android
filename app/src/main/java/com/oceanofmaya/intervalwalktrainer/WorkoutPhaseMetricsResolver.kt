package com.oceanofmaya.intervalwalktrainer

fun WorkoutSession.hasPhaseMetricsContext(): Boolean {
    return !formulaSnapshotJson.isNullOrBlank() ||
        !metricsIntervalsJson.isNullOrBlank() ||
        startedAt != null
}

object WorkoutPhaseMetricsResolver {
    fun resolvePhaseWindows(session: WorkoutSession): List<WorkoutPhaseWindow> {
        val rebuiltWindows = rebuildPhaseWindows(session)
        if (rebuiltWindows.isNotEmpty()) {
            return rebuiltWindows
        }
        return WorkoutMetricsCodec.decodePhaseWindows(session.metricsPhaseWindowsJson)
    }

    private fun rebuildPhaseWindows(session: WorkoutSession): List<WorkoutPhaseWindow> {
        val snapshot = WorkoutMetricsCodec.decodeFormulaSnapshot(session.formulaSnapshotJson)
        val intervals = WorkoutMetricsIntervalCodec.resolvePhaseMappingIntervals(session)
        if (snapshot == null || intervals.isEmpty()) {
            return emptyList()
        }
        val phases = WorkoutPhaseSequenceBuilder.build(snapshot.toFormula(session.workoutType))
        return WorkoutPhaseTimelineMapper.map(phases, intervals)
    }
}
