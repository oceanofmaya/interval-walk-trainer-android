package com.oceanofmaya.intervalwalktrainer

object WorkoutPhaseSequenceBuilder {
    fun build(formula: IntervalFormula): List<WorkoutPhaseSegment> {
        return if (formula.isCircuit) {
            buildCircuitSequence(formula)
        } else {
            buildIntervalSequence(formula)
        }
    }

    private fun buildIntervalSequence(formula: IntervalFormula): List<WorkoutPhaseSegment> {
        val segments = mutableListOf<WorkoutPhaseSegment>()
        if (formula.startsWithFast) {
            segments += phase(formula, WorkoutPhaseType.FAST)
            for (interval in 2..formula.totalIntervals) {
                segments += phase(formula, WorkoutPhaseType.SLOW)
                segments += phase(formula, WorkoutPhaseType.FAST)
            }
            segments += phase(formula, WorkoutPhaseType.SLOW)
        } else {
            repeat(formula.totalIntervals) {
                segments += phase(formula, WorkoutPhaseType.SLOW)
                segments += phase(formula, WorkoutPhaseType.FAST)
            }
        }
        return segments
    }

    private fun buildCircuitSequence(formula: IntervalFormula): List<WorkoutPhaseSegment> {
        val rounds = formula.totalIntervals / 2
        val segments = mutableListOf<WorkoutPhaseSegment>()
        repeat(rounds) {
            if (formula.startsWithFast) {
                segments += phase(formula, WorkoutPhaseType.FAST)
                segments += phase(formula, WorkoutPhaseType.SLOW)
                segments += phase(formula, WorkoutPhaseType.FAST)
            } else {
                segments += phase(formula, WorkoutPhaseType.SLOW)
                segments += phase(formula, WorkoutPhaseType.FAST)
                segments += phase(formula, WorkoutPhaseType.SLOW)
            }
        }
        return segments
    }

    private fun phase(formula: IntervalFormula, phase: WorkoutPhaseType): WorkoutPhaseSegment {
        val durationSeconds = when (phase) {
            WorkoutPhaseType.SLOW -> formula.slowDurationSeconds
            WorkoutPhaseType.FAST -> formula.fastDurationSeconds
        }
        return WorkoutPhaseSegment(phase, durationSeconds)
    }
}
