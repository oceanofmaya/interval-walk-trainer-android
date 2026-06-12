package com.oceanofmaya.intervalwalktrainer

data class WorkoutPhaseSegment(
    val phase: WorkoutPhaseType,
    val durationSeconds: Int
)

data class WorkoutPhaseWindow(
    val phase: WorkoutPhaseType,
    val startedAtMillis: Long,
    val endedAtMillis: Long
)

data class WorkoutFormulaSnapshot(
    val slowDurationSeconds: Int,
    val fastDurationSeconds: Int,
    val totalIntervals: Int,
    val startsWithFast: Boolean,
    val isCircuit: Boolean
) {
    fun toFormula(name: String = ""): IntervalFormula {
        return IntervalFormula(
            name = name,
            slowDurationSeconds = slowDurationSeconds,
            fastDurationSeconds = fastDurationSeconds,
            totalIntervals = totalIntervals,
            startsWithFast = startsWithFast,
            isCircuit = isCircuit
        )
    }

    companion object {
        fun fromFormula(formula: IntervalFormula): WorkoutFormulaSnapshot {
            return WorkoutFormulaSnapshot(
                slowDurationSeconds = formula.slowDurationSeconds,
                fastDurationSeconds = formula.fastDurationSeconds,
                totalIntervals = formula.totalIntervals,
                startsWithFast = formula.startsWithFast,
                isCircuit = formula.isCircuit
            )
        }
    }
}

data class PhaseHeartRateSummary(
    val fastPhaseAverageBpm: Int? = null,
    val slowPhaseAverageBpm: Int? = null
)
