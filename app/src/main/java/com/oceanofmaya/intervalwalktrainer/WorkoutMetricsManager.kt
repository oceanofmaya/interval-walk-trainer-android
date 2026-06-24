package com.oceanofmaya.intervalwalktrainer

import android.content.Context

class WorkoutMetricsManager(context: Context) {
    private val healthConnectMetricsSource = HealthConnectMetricsSource(context.applicationContext)
    private val activeIntervals = mutableListOf<WorkoutMetricsInterval>()
    private var sessionStartedAtMillis: Long? = null
    private var activeIntervalStartedAtMillis: Long? = null
    private var running = false

    fun healthConnectAvailable(): Boolean = healthConnectMetricsSource.isAvailable()

    suspend fun hasAnyHealthConnectMetricsPermission(): Boolean {
        return healthConnectMetricsSource.hasAnyPermission()
    }

    suspend fun missingHealthConnectMetricsPermissions(): Set<String> {
        return healthConnectMetricsSource.missingPermissions()
    }

    fun healthConnectMetricsPermissions(): Set<String> {
        return healthConnectMetricsSource.requiredPermissions
    }

    suspend fun missingHealthConnectStepPermissions(): Set<String> {
        return healthConnectMetricsSource.missingStepPermissions()
    }

    suspend fun missingHealthConnectHeartRatePermissions(): Set<String> {
        return healthConnectMetricsSource.missingHeartRatePermissions()
    }

    fun snapshot(): WorkoutMetricsSessionSnapshot? {
        val startedAt = sessionStartedAtMillis ?: return null
        return WorkoutMetricsSessionSnapshot(
            sessionStartedAtMillis = startedAt,
            activeIntervals = activeIntervals.toList(),
            activeIntervalStartedAtMillis = activeIntervalStartedAtMillis,
            running = running
        )
    }

    fun restore(snapshot: WorkoutMetricsSessionSnapshot) {
        sessionStartedAtMillis = snapshot.sessionStartedAtMillis
        activeIntervals.clear()
        activeIntervals.addAll(snapshot.activeIntervals)
        activeIntervalStartedAtMillis = snapshot.activeIntervalStartedAtMillis
        running = snapshot.running
    }

    fun restoreFromElapsedSeconds(
        elapsedSeconds: Int,
        metricsEnabled: Boolean,
        nowMillis: Long = System.currentTimeMillis()
    ) {
        if (!metricsEnabled || elapsedSeconds <= 0 || sessionStartedAtMillis != null) {
            return
        }
        val sessionStart = (nowMillis - elapsedSeconds * 1000L).coerceAtLeast(0L)
        sessionStartedAtMillis = sessionStart
        activeIntervals.clear()
        if (nowMillis > sessionStart) {
            activeIntervals += WorkoutMetricsInterval(sessionStart, nowMillis)
        }
        activeIntervalStartedAtMillis = nowMillis
        running = true
    }

    fun startOrResumeSession(metricsEnabled: Boolean, nowMillis: Long = System.currentTimeMillis()) {
        if (!metricsEnabled) {
            clearSession()
            return
        }
        if (running) return
        if (sessionStartedAtMillis == null) {
            sessionStartedAtMillis = nowMillis
        }
        activeIntervalStartedAtMillis = nowMillis
        running = true
    }

    fun pauseSession(nowMillis: Long = System.currentTimeMillis()) {
        if (!running) return
        closeActiveInterval(nowMillis)
        running = false
    }

    fun clearSession() {
        activeIntervals.clear()
        sessionStartedAtMillis = null
        activeIntervalStartedAtMillis = null
        running = false
    }

    suspend fun completeSession(
        metricsEnabled: Boolean,
        formula: IntervalFormula,
        nowMillis: Long = System.currentTimeMillis()
    ): WorkoutMetricsSummary? {
        val startedAt = sessionStartedAtMillis
        if (!metricsEnabled || startedAt == null) {
            clearSession()
            return null
        }
        val summary = completeActiveSession(startedAt, formula, nowMillis)
        clearSession()
        return summary
    }

    private suspend fun completeActiveSession(
        startedAt: Long,
        formula: IntervalFormula,
        nowMillis: Long
    ): WorkoutMetricsSummary? {
        if (running) closeActiveInterval(nowMillis)
        running = false
        val trackedIntervals = activeIntervals.toList()
        val expectedDurationMillis = formula.totalDurationSeconds * 1000L
        val canonicalStartedAt = WorkoutMetricsIntervalCodec.canonicalSessionStartedAt(
            trackedStartedAtMillis = startedAt,
            completedAtMillis = nowMillis,
            expectedDurationMillis = expectedDurationMillis
        )
        val readIntervals = WorkoutMetricsIntervalCodec.resolveReadIntervals(
            trackedIntervals = trackedIntervals,
            sessionStartedAtMillis = canonicalStartedAt,
            completedAtMillis = nowMillis,
            expectedDurationMillis = expectedDurationMillis
        )
        val phaseMappingIntervals = if (
            trackedIntervals.isNotEmpty() &&
            trackedIntervals.sumOf { it.durationMillis } >= expectedDurationMillis *
            PHASE_MAPPING_COVERAGE_THRESHOLD
        ) {
            trackedIntervals
        } else {
            readIntervals
        }
        val phaseWindows = buildPhaseWindows(formula, phaseMappingIntervals)
        val healthConnectMetricsSummary = readHealthConnectSummary(readIntervals, phaseWindows)
        return (healthConnectMetricsSummary ?: WorkoutMetricsSummary()).copy(
            startedAt = canonicalStartedAt,
            intervals = trackedIntervals,
            formulaSnapshot = WorkoutFormulaSnapshot.fromFormula(formula),
            phaseWindows = phaseWindows
        ).takeIf { it.hasDisplayableValue || it.startedAt != null }
    }

    private fun buildPhaseWindows(
        formula: IntervalFormula,
        intervals: List<WorkoutMetricsInterval>
    ): List<WorkoutPhaseWindow> {
        if (intervals.isEmpty()) return emptyList()
        val phases = WorkoutPhaseSequenceBuilder.build(formula)
        return WorkoutPhaseTimelineMapper.map(phases, intervals)
    }

    private suspend fun readHealthConnectSummary(
        intervals: List<WorkoutMetricsInterval>,
        phaseWindows: List<WorkoutPhaseWindow>
    ): WorkoutMetricsSummary? {
        return runCatching {
            healthConnectMetricsSource.readSummary(intervals, phaseWindows)
        }.getOrNull()
    }

    private companion object {
        const val PHASE_MAPPING_COVERAGE_THRESHOLD = 0.75
    }

    private fun closeActiveInterval(nowMillis: Long) {
        val startedAt = activeIntervalStartedAtMillis ?: return
        if (nowMillis > startedAt) {
            activeIntervals += WorkoutMetricsInterval(startedAt, nowMillis)
        }
        activeIntervalStartedAtMillis = null
    }
}
