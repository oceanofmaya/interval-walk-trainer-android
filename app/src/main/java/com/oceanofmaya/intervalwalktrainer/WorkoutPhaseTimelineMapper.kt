package com.oceanofmaya.intervalwalktrainer

object WorkoutPhaseTimelineMapper {
    fun map(
        phases: List<WorkoutPhaseSegment>,
        activeIntervals: List<WorkoutMetricsInterval>
    ): List<WorkoutPhaseWindow> {
        if (phases.isEmpty() || activeIntervals.isEmpty()) return emptyList()

        val windows = mutableListOf<WorkoutPhaseWindow>()
        var phaseIndex = 0
        var phaseRemainingMillis = phases.first().durationSeconds * 1000L
        var currentPhase = phases.first().phase

        for (interval in activeIntervals.sortedBy { it.startedAtMillis }) {
            var cursor = interval.startedAtMillis
            val intervalEnd = interval.endedAtMillis
            while (cursor < intervalEnd && phaseIndex < phases.size) {
                val availableMillis = intervalEnd - cursor
                val sliceMillis = minOf(availableMillis, phaseRemainingMillis)
                if (sliceMillis <= 0L) break

                val windowEnd = cursor + sliceMillis
                appendWindow(windows, WorkoutPhaseWindow(currentPhase, cursor, windowEnd))
                cursor = windowEnd
                phaseRemainingMillis -= sliceMillis
                if (phaseRemainingMillis <= 0L) {
                    phaseIndex++
                    if (phaseIndex < phases.size) {
                        currentPhase = phases[phaseIndex].phase
                        phaseRemainingMillis = phases[phaseIndex].durationSeconds * 1000L
                    }
                }
            }
        }

        return windows
    }

    private fun appendWindow(windows: MutableList<WorkoutPhaseWindow>, window: WorkoutPhaseWindow) {
        val last = windows.lastOrNull()
        if (last != null &&
            last.phase == window.phase &&
            last.endedAtMillis == window.startedAtMillis
        ) {
            windows[windows.lastIndex] = last.copy(endedAtMillis = window.endedAtMillis)
        } else {
            windows += window
        }
    }
}
