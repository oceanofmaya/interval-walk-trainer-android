package com.oceanofmaya.intervalwalktrainer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SavedWorkoutMigrationTest {

    private val intervalFormula = IntervalFormula(
        name = "Custom: 3-3 x 5",
        slowDurationSeconds = 180,
        fastDurationSeconds = 180,
        totalIntervals = 5,
        startsWithFast = false,
        isCircuit = false
    )

    private val circuitFormula = intervalFormula.copy(
        name = "Custom Circuit: 5-4-5 x 2",
        isCircuit = true,
        totalIntervals = 6,
        startsWithFast = true
    )

    @Test
    fun `skips when already migrated`() {
        val result = SavedWorkoutMigration.decide(
            alreadyMigrated = true,
            hasCustomFormula = true,
            legacyFormula = intervalFormula,
            circuitPattern = SavedWorkoutRepository.CIRCUIT_PATTERN_FAST_SLOW_FAST,
            savedCount = 0
        )
        assertSame(SavedWorkoutMigration.Decision.Skip, result)
    }

    @Test
    fun `skips when there is no custom formula in prefs`() {
        val result = SavedWorkoutMigration.decide(
            alreadyMigrated = false,
            hasCustomFormula = false,
            legacyFormula = null,
            circuitPattern = SavedWorkoutRepository.CIRCUIT_PATTERN_FAST_SLOW_FAST,
            savedCount = 0
        )
        assertSame(SavedWorkoutMigration.Decision.Skip, result)
    }

    @Test
    fun `marks migrated when custom flag is set but legacy formula is unrecoverable`() {
        // If the legacy custom flag is true but the stored payload is corrupt/unreadable,
        // we cannot insert anything - but we also must not keep re-evaluating on every cold start.
        val result = SavedWorkoutMigration.decide(
            alreadyMigrated = false,
            hasCustomFormula = true,
            legacyFormula = null,
            circuitPattern = SavedWorkoutRepository.CIRCUIT_PATTERN_FAST_SLOW_FAST,
            savedCount = 0
        )
        assertSame(SavedWorkoutMigration.Decision.MarkMigratedOnly, result)
    }

    @Test
    fun `marks migrated only when at cap to avoid losing the legacy formula and re-evaluating forever`() {
        val result = SavedWorkoutMigration.decide(
            alreadyMigrated = false,
            hasCustomFormula = true,
            legacyFormula = intervalFormula,
            circuitPattern = SavedWorkoutRepository.CIRCUIT_PATTERN_FAST_SLOW_FAST,
            savedCount = SavedWorkoutRepository.MAX_SAVED_WORKOUTS
        )
        assertSame(SavedWorkoutMigration.Decision.MarkMigratedOnly, result)
    }

    @Test
    fun `produces an insert entity when below cap for interval formulas`() {
        val result = SavedWorkoutMigration.decide(
            alreadyMigrated = false,
            hasCustomFormula = true,
            legacyFormula = intervalFormula,
            circuitPattern = SavedWorkoutRepository.CIRCUIT_PATTERN_SLOW_FAST_SLOW,
            savedCount = 0
        )
        assertTrue(result is SavedWorkoutMigration.Decision.InsertAndMark)
        val entity = (result as SavedWorkoutMigration.Decision.InsertAndMark).entity
        assertEquals(intervalFormula.name, entity.displayName)
        assertEquals(intervalFormula.slowDurationSeconds, entity.slowDurationSeconds)
        assertEquals(intervalFormula.fastDurationSeconds, entity.fastDurationSeconds)
        assertEquals(intervalFormula.totalIntervals, entity.totalIntervals)
        assertEquals(intervalFormula.startsWithFast, entity.startsWithFast)
        assertEquals(intervalFormula.isCircuit, entity.isCircuit)
        // Interval formulas should always store the canonical placeholder pattern.
        assertEquals(SavedWorkoutRepository.CIRCUIT_PATTERN_FAST_SLOW_FAST, entity.circuitPattern)
        assertEquals(0, entity.sortOrder)
    }

    @Test
    fun `preserves the explicit circuit pattern for circuit formulas`() {
        val result = SavedWorkoutMigration.decide(
            alreadyMigrated = false,
            hasCustomFormula = true,
            legacyFormula = circuitFormula,
            circuitPattern = SavedWorkoutRepository.CIRCUIT_PATTERN_SLOW_FAST_SLOW,
            savedCount = 5
        )
        assertTrue(result is SavedWorkoutMigration.Decision.InsertAndMark)
        val entity = (result as SavedWorkoutMigration.Decision.InsertAndMark).entity
        assertTrue(entity.isCircuit)
        assertEquals(SavedWorkoutRepository.CIRCUIT_PATTERN_SLOW_FAST_SLOW, entity.circuitPattern)
    }

    @Test
    fun `respects a custom cap below the default limit`() {
        val result = SavedWorkoutMigration.decide(
            alreadyMigrated = false,
            hasCustomFormula = true,
            legacyFormula = intervalFormula,
            circuitPattern = SavedWorkoutRepository.CIRCUIT_PATTERN_FAST_SLOW_FAST,
            savedCount = 3,
            cap = 3
        )
        assertSame(SavedWorkoutMigration.Decision.MarkMigratedOnly, result)
    }
}
