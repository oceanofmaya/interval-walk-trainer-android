package com.oceanofmaya.intervalwalktrainer

object WorkoutMetricsCodec {
    private const val FIELD_SEPARATOR = ";"
    private const val VALUE_SEPARATOR = ","
    private const val FORMULA_FIELD_COUNT = 5
    private const val PHASE_WINDOW_FIELD_COUNT = 3

    fun encodeFormulaSnapshot(snapshot: WorkoutFormulaSnapshot): String {
        return listOf(
            snapshot.slowDurationSeconds,
            snapshot.fastDurationSeconds,
            snapshot.totalIntervals,
            if (snapshot.startsWithFast) 1 else 0,
            if (snapshot.isCircuit) 1 else 0
        ).joinToString(VALUE_SEPARATOR)
    }

    fun decodeFormulaSnapshot(encoded: String?): WorkoutFormulaSnapshot? {
        return encoded?.takeIf { it.isNotBlank() }
            ?.split(VALUE_SEPARATOR)
            ?.takeIf { it.size == FORMULA_FIELD_COUNT }
            ?.let { values ->
                val slow = values[0].toIntOrNull()
                val fast = values[1].toIntOrNull()
                val intervals = values[2].toIntOrNull()
                if (slow == null || fast == null || intervals == null) {
                    null
                } else {
                    WorkoutFormulaSnapshot(
                        slowDurationSeconds = slow,
                        fastDurationSeconds = fast,
                        totalIntervals = intervals,
                        startsWithFast = values[3] == "1",
                        isCircuit = values[4] == "1"
                    )
                }
            }
    }

    fun encodePhaseWindows(windows: List<WorkoutPhaseWindow>): String? {
        if (windows.isEmpty()) return null
        return windows.joinToString(FIELD_SEPARATOR) { window ->
            listOf(
                window.phase.name.lowercase(),
                window.startedAtMillis,
                window.endedAtMillis
            ).joinToString(VALUE_SEPARATOR)
        }
    }

    fun decodePhaseWindows(encoded: String?): List<WorkoutPhaseWindow> {
        if (encoded.isNullOrBlank()) return emptyList()
        return encoded.split(FIELD_SEPARATOR).mapNotNull { part ->
            val values = part.split(VALUE_SEPARATOR)
            if (values.size != PHASE_WINDOW_FIELD_COUNT) return@mapNotNull null
            val phase = when (values[0].lowercase()) {
                "slow" -> WorkoutPhaseType.SLOW
                "fast" -> WorkoutPhaseType.FAST
                else -> return@mapNotNull null
            }
            val start = values[1].toLongOrNull() ?: return@mapNotNull null
            val end = values[2].toLongOrNull() ?: return@mapNotNull null
            if (end <= start) return@mapNotNull null
            WorkoutPhaseWindow(phase, start, end)
        }
    }
}
