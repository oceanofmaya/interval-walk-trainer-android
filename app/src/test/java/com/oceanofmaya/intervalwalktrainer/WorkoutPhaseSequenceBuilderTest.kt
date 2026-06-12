package com.oceanofmaya.intervalwalktrainer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WorkoutPhaseSequenceBuilderTest {
    @Test
    fun `builds slow start interval sequence`() {
        val formula = IntervalFormula(
            name = "3-3",
            slowDurationSeconds = 180,
            fastDurationSeconds = 180,
            totalIntervals = 2
        )

        val phases = WorkoutPhaseSequenceBuilder.build(formula)

        assertEquals(
            listOf(
                WorkoutPhaseType.SLOW,
                WorkoutPhaseType.FAST,
                WorkoutPhaseType.SLOW,
                WorkoutPhaseType.FAST
            ),
            phases.map { it.phase }
        )
    }

    @Test
    fun `builds fast start interval sequence with trailing slow`() {
        val formula = IntervalFormulas.formula3.copy(totalIntervals = 2)

        val phases = WorkoutPhaseSequenceBuilder.build(formula)

        assertEquals(
            listOf(
                WorkoutPhaseType.FAST,
                WorkoutPhaseType.SLOW,
                WorkoutPhaseType.FAST,
                WorkoutPhaseType.SLOW
            ),
            phases.map { it.phase }
        )
    }

    @Test
    fun `builds fast start circuit sequence`() {
        val phases = WorkoutPhaseSequenceBuilder.build(IntervalFormulas.formula4)

        assertEquals(
            listOf(
                WorkoutPhaseType.FAST,
                WorkoutPhaseType.SLOW,
                WorkoutPhaseType.FAST,
                WorkoutPhaseType.FAST,
                WorkoutPhaseType.SLOW,
                WorkoutPhaseType.FAST
            ),
            phases.map { it.phase }
        )
    }
}
