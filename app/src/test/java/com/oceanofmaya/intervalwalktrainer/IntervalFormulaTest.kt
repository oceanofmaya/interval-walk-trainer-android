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
        // 5-4-5 Circuit - 2 Rounds: 2 * (2*300 + 240) = 1680 seconds (28 min)
        assertEquals(1680, IntervalFormulas.formula4.totalDurationSeconds)
        assertEquals(4, IntervalFormulas.formula4.totalIntervals)
        assertEquals(true, IntervalFormulas.formula4.startsWithFast)
        assertEquals(true, IntervalFormulas.formula4.isCircuit)
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
        // Circuit: Fast(5) → Slow(4) → Fast(5) × 2 rounds; 2 * (2*300 + 240) = 1680 seconds (28 min)
        val circuitFormula = IntervalFormula(
            name = "Custom Circuit: 5-4-5 - 2 Rounds",
            slowDurationSeconds = 4 * 60, // 4 minutes
            fastDurationSeconds = 5 * 60, // 5 minutes
            totalIntervals = 4, // 2 circuits × 2 intervals per circuit
            startsWithFast = true, // Fast-Slow-Fast pattern
            isCircuit = true
        )
        assertEquals(1680, circuitFormula.totalDurationSeconds)
        assertEquals(4, circuitFormula.totalIntervals)
        assertEquals(true, circuitFormula.startsWithFast)
        assertEquals(true, circuitFormula.isCircuit)
    }

    @Test
    fun `custom circuit formula with slow-fast-slow pattern calculates correctly`() {
        // Circuit: Slow(3) → Fast(2) → Slow(3) × 3 rounds; 3 * (2*180 + 120) = 1440 seconds (24 min)
        val circuitFormula = IntervalFormula(
            name = "Custom Circuit: 3-2-3 - 3 Rounds",
            slowDurationSeconds = 3 * 60, // 3 minutes
            fastDurationSeconds = 2 * 60, // 2 minutes
            totalIntervals = 6, // 3 circuits × 2 intervals per circuit
            startsWithFast = false, // Slow-Fast-Slow pattern
            isCircuit = true
        )
        assertEquals(1440, circuitFormula.totalDurationSeconds)
        assertEquals(6, circuitFormula.totalIntervals)
        assertEquals(false, circuitFormula.startsWithFast)
        assertEquals(true, circuitFormula.isCircuit)
    }

    @Test
    fun `circuit formula rounds calculation is correct`() {
        val circuit2Rounds = IntervalFormula(
            name = "Circuit - 2 Rounds",
            slowDurationSeconds = 4 * 60,
            fastDurationSeconds = 5 * 60,
            totalIntervals = 4,
            startsWithFast = true,
            isCircuit = true
        )
        assertEquals(2, circuit2Rounds.totalIntervals / 2)
        assertEquals(1680, circuit2Rounds.totalDurationSeconds) // 2 * (5+4+5)*60

        val circuit5Rounds = IntervalFormula(
            name = "Circuit - 5 Rounds",
            slowDurationSeconds = 3 * 60,
            fastDurationSeconds = 3 * 60,
            totalIntervals = 10,
            startsWithFast = false,
            isCircuit = true
        )
        assertEquals(5, circuit5Rounds.totalIntervals / 2)
        assertEquals(5 * (2 * 180 + 180), circuit5Rounds.totalDurationSeconds) // 5 * (3+3+3)*60 = 2700
    }
}

