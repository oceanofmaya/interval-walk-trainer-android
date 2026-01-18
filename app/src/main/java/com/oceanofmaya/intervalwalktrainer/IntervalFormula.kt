package com.oceanofmaya.intervalwalktrainer

/**
 * Represents an interval training formula.
 * 
 * @param name Display name of the formula
 * @param slowDurationSeconds Duration of slow/recovery phase in seconds
 * @param fastDurationSeconds Duration of fast/high-intensity phase in seconds
 * @param totalIntervals Number of complete intervals (each interval = slow + fast)
 * @param startsWithFast Whether the workout starts with fast phase (default: false, starts with slow)
 */
data class IntervalFormula(
    val name: String,
    val slowDurationSeconds: Int,
    val fastDurationSeconds: Int,
    val totalIntervals: Int,
    val startsWithFast: Boolean = false
) {
    /**
     * Total duration of the workout in seconds.
     * Calculated as (slow + fast) * totalIntervals.
     */
    val totalDurationSeconds: Int
        get() = (slowDurationSeconds + fastDurationSeconds) * totalIntervals
}

/**
 * Predefined interval training formulas.
 * 
 * Provides three ready-to-use training formulas covering the main training patterns.
 * Additional variations can be created using the "Design Your Own" option.
 * All formulas follow a consistent naming pattern and include duration information.
 */
object IntervalFormulas {
    /**
     * Standard: The classic Japanese Interval Walking Training (IWT) method.
     * 3-3 Japanese pattern with 5 rounds (30 minutes total).
     * This is the default formula.
     */
    val formula2 = IntervalFormula(
        name = "3-3 Japanese - 5 Rounds (30 min)",
        slowDurationSeconds = 3 * 60, // 3 minutes
        fastDurationSeconds = 3 * 60, // 3 minutes
        totalIntervals = 5
    )

    /**
     * High-intensity training: Four rounds of 5-minute high-intensity and 2-minute recovery.
     * 5-2 pattern starting with fast phase (28 minutes total).
     */
    val formula3 = IntervalFormula(
        name = "5-2 High Intensity - 4 Rounds (28 min)",
        slowDurationSeconds = 2 * 60, // 2 minutes low-intensity recovery
        fastDurationSeconds = 5 * 60, // 5 minutes high-intensity
        totalIntervals = 4, // Four rounds
        startsWithFast = true // Pattern: Fast(5) → Slow(2)
    )

    /**
     * Circuit training: 5 min brisk, 4 min slow, 5 min brisk sequence.
     * 5-4-5 pattern repeated 2 times (36 minutes total).
     */
    val formula4 = IntervalFormula(
        name = "5-4-5 Circuit - 2 Rounds (36 min)",
        slowDurationSeconds = 4 * 60, // 4 minutes slow recovery
        fastDurationSeconds = 5 * 60, // 5 minutes brisk
        totalIntervals = 4, // 2 circuits (5-4-5 pattern repeated twice)
        startsWithFast = true // Pattern: Fast(5) → Slow(4) → Fast(5)
    )

    /** All available training formulas. */
    val all = listOf(formula2, formula3, formula4)
    
    /** Default formula: 3-3 Japanese - 5 Rounds (30 minutes). */
    val default = formula2
}

