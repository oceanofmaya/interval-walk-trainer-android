package com.oceanofmaya.intervalwalktrainer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SavedWorkoutTest {

    @Test
    fun `toIntervalFormula maps stored fields to IntervalFormula`() {
        val saved = SavedWorkout(
            id = 42L,
            displayName = "Morning walk",
            slowDurationSeconds = 180,
            fastDurationSeconds = 120,
            totalIntervals = 6,
            isCircuit = true,
            circuitPattern = SavedWorkoutRepository.CIRCUIT_PATTERN_SLOW_FAST_SLOW,
            startsWithFast = true,
            createdAt = 1L,
            sortOrder = 3
        )
        val f = saved.toIntervalFormula()
        assertEquals("Morning walk", f.name)
        assertEquals(180, f.slowDurationSeconds)
        assertEquals(120, f.fastDurationSeconds)
        assertEquals(6, f.totalIntervals)
        assertTrue(f.isCircuit)
        assertTrue(f.startsWithFast)
    }

    @Test
    fun `toIntervalFormula preserves interval mode`() {
        val saved = SavedWorkout(
            displayName = "Intervals",
            slowDurationSeconds = 60,
            fastDurationSeconds = 60,
            totalIntervals = 10,
            isCircuit = false,
            circuitPattern = SavedWorkoutRepository.CIRCUIT_PATTERN_FAST_SLOW_FAST,
            startsWithFast = false
        )
        val f = saved.toIntervalFormula()
        assertFalse(f.isCircuit)
        assertFalse(f.startsWithFast)
    }
}
