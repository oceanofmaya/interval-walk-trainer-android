package com.oceanofmaya.intervalwalktrainer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for IntervalFormula data class and IntervalFormulas object.
 * 
 * Tests cover:
 * - Duration calculations (totalDurationSeconds)
 * - All predefined formulas (formula2, formula3, formula4)
 * - Edge cases (single interval, asymmetric durations)
 */
class IntervalFormulaTest {

    @Test
    fun `totalDurationSeconds calculates correctly for simple formula`() {
        val formula = IntervalFormula(
            name = "Test",
            slowDurationSeconds = 180, // 3 minutes
            fastDurationSeconds = 180, // 3 minutes
            totalIntervals = 3
        )
        
        // Expected: (180 + 180) * 3 = 1080 seconds = 18 minutes
        assertEquals(1080, formula.totalDurationSeconds)
    }

    @Test
    fun `totalDurationSeconds calculates correctly for asymmetric formula`() {
        val formula = IntervalFormula(
            name = "Test",
            slowDurationSeconds = 120, // 2 minutes
            fastDurationSeconds = 300, // 5 minutes
            totalIntervals = 4
        )
        
        // Expected: (120 + 300) * 4 = 1680 seconds = 28 minutes
        assertEquals(1680, formula.totalDurationSeconds)
    }

    @Test
    fun `totalDurationSeconds handles single interval`() {
        val formula = IntervalFormula(
            name = "Test",
            slowDurationSeconds = 60,
            fastDurationSeconds = 60,
            totalIntervals = 1
        )
        
        assertEquals(120, formula.totalDurationSeconds)
    }

    @Test
    fun `formula2 has correct total duration`() {
        // 3-3 Japanese - 5 Rounds: (180 + 180) * 5 = 1800 seconds
        assertEquals(1800, IntervalFormulas.formula2.totalDurationSeconds)
        assertEquals(5, IntervalFormulas.formula2.totalIntervals)
    }

    @Test
    fun `formula3 has correct total duration and starts with fast`() {
        // 5-2 High Intensity - 4 Rounds: (120 + 300) * 4 = 1680 seconds
        assertEquals(1680, IntervalFormulas.formula3.totalDurationSeconds)
        assertEquals(4, IntervalFormulas.formula3.totalIntervals)
        assertEquals(120, IntervalFormulas.formula3.slowDurationSeconds)
        assertEquals(300, IntervalFormulas.formula3.fastDurationSeconds)
        assertEquals(true, IntervalFormulas.formula3.startsWithFast)
    }

    @Test
    fun `formula4 has correct total duration`() {
        // 5-4-5 Circuit - 2 Rounds: (240 + 300) * 4 = 2160 seconds
        // Note: totalIntervals is 4 because each circuit has 2 intervals (fast-slow-fast)
        assertEquals(2160, IntervalFormulas.formula4.totalDurationSeconds)
        assertEquals(4, IntervalFormulas.formula4.totalIntervals)
        assertEquals(true, IntervalFormulas.formula4.startsWithFast)
    }

    @Test
    fun `all formulas list contains all formulas`() {
        assertEquals(3, IntervalFormulas.all.size)
        assertTrue(IntervalFormulas.all.contains(IntervalFormulas.formula2))
        assertTrue(IntervalFormulas.all.contains(IntervalFormulas.formula3))
        assertTrue(IntervalFormulas.all.contains(IntervalFormulas.formula4))
    }

    @Test
    fun `default formula is formula2`() {
        assertEquals(IntervalFormulas.formula2, IntervalFormulas.default)
    }

    @Test
    fun `custom formula with startsWithFast calculates correctly`() {
        val customFormula = IntervalFormula(
            name = "Custom: 5-3 - 4 Rounds",
            slowDurationSeconds = 3 * 60, // 3 minutes
            fastDurationSeconds = 5 * 60, // 5 minutes
            totalIntervals = 4,
            startsWithFast = true
        )
        
        // Expected: (180 + 300) * 4 = 1920 seconds = 32 minutes
        assertEquals(1920, customFormula.totalDurationSeconds)
        assertEquals(true, customFormula.startsWithFast)
    }

    @Test
    fun `custom formula with startsWithFast false calculates correctly`() {
        val customFormula = IntervalFormula(
            name = "Custom: 2-4 - 3 Rounds",
            slowDurationSeconds = 2 * 60, // 2 minutes
            fastDurationSeconds = 4 * 60, // 4 minutes
            totalIntervals = 3,
            startsWithFast = false
        )
        
        // Expected: (120 + 240) * 3 = 1080 seconds = 18 minutes
        assertEquals(1080, customFormula.totalDurationSeconds)
        assertEquals(false, customFormula.startsWithFast)
    }

    @Test
    fun `custom formula with single round calculates correctly`() {
        val customFormula = IntervalFormula(
            name = "Custom: 1-1 - 1 Round",
            slowDurationSeconds = 60,
            fastDurationSeconds = 60,
            totalIntervals = 1,
            startsWithFast = true
        )
        
        assertEquals(120, customFormula.totalDurationSeconds)
    }

    @Test
    fun `custom circuit formula with fast-slow-fast pattern calculates correctly`() {
        // Circuit: Fast(5) → Slow(4) → Fast(5) × 2 rounds
        // Each circuit = 2 intervals, so totalIntervals = rounds * 2
        val circuitFormula = IntervalFormula(
            name = "Custom Circuit: 5-4-5 - 2 Rounds",
            slowDurationSeconds = 4 * 60, // 4 minutes
            fastDurationSeconds = 5 * 60, // 5 minutes
            totalIntervals = 4, // 2 circuits × 2 intervals per circuit
            startsWithFast = true // Fast-Slow-Fast pattern
        )
        
        // Expected: (240 + 300) * 4 = 2160 seconds = 36 minutes
        assertEquals(2160, circuitFormula.totalDurationSeconds)
        assertEquals(4, circuitFormula.totalIntervals)
        assertEquals(true, circuitFormula.startsWithFast)
    }

    @Test
    fun `custom circuit formula with slow-fast-slow pattern calculates correctly`() {
        // Circuit: Slow(3) → Fast(2) → Slow(3) × 3 rounds
        val circuitFormula = IntervalFormula(
            name = "Custom Circuit: 3-2-3 - 3 Rounds",
            slowDurationSeconds = 3 * 60, // 3 minutes
            fastDurationSeconds = 2 * 60, // 2 minutes
            totalIntervals = 6, // 3 circuits × 2 intervals per circuit
            startsWithFast = false // Slow-Fast-Slow pattern
        )
        
        // Expected: (180 + 120) * 6 = 1800 seconds = 30 minutes
        assertEquals(1800, circuitFormula.totalDurationSeconds)
        assertEquals(6, circuitFormula.totalIntervals)
        assertEquals(false, circuitFormula.startsWithFast)
    }

    @Test
    fun `circuit formula rounds calculation is correct`() {
        // Verify that rounds = totalIntervals / 2 for circuits
        val circuit2Rounds = IntervalFormula(
            name = "Circuit - 2 Rounds",
            slowDurationSeconds = 4 * 60,
            fastDurationSeconds = 5 * 60,
            totalIntervals = 4, // 2 rounds × 2 intervals
            startsWithFast = true
        )
        assertEquals(2, circuit2Rounds.totalIntervals / 2)
        
        val circuit5Rounds = IntervalFormula(
            name = "Circuit - 5 Rounds",
            slowDurationSeconds = 3 * 60,
            fastDurationSeconds = 3 * 60,
            totalIntervals = 10, // 5 rounds × 2 intervals
            startsWithFast = false
        )
        assertEquals(5, circuit5Rounds.totalIntervals / 2)
    }
}

