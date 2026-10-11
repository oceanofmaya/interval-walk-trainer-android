package com.oceanofmaya.intervalwalktrainer

/**
 * A phone/watch-compatible interval training formula.
 */
data class IntervalFormula(
    val name: String,
    val slowDurationSeconds: Int,
    val fastDurationSeconds: Int,
    val totalIntervals: Int,
    val startsWithFast: Boolean = false,
    val isCircuit: Boolean = false
) {
    val totalDurationSeconds: Int
        get() = if (isCircuit) {
            val rounds = totalIntervals / 2
            if (startsWithFast) {
                rounds * (2 * fastDurationSeconds + slowDurationSeconds)
            } else {
                rounds * (2 * slowDurationSeconds + fastDurationSeconds)
            }
        } else {
            (slowDurationSeconds + fastDurationSeconds) * totalIntervals
        }
}

object IntervalFormulas {
    val formula2 = IntervalFormula(
        name = "3-3 Japanese - 5 Rounds",
        slowDurationSeconds = 3 * 60,
        fastDurationSeconds = 3 * 60,
        totalIntervals = 5
    )

    val formula3 = IntervalFormula(
        name = "5-2 High Intensity - 4 Rounds",
        slowDurationSeconds = 2 * 60,
        fastDurationSeconds = 5 * 60,
        totalIntervals = 4,
        startsWithFast = true
    )

    val formula4 = IntervalFormula(
        name = "5-4-5 Circuit - 2 Rounds",
        slowDurationSeconds = 4 * 60,
        fastDurationSeconds = 5 * 60,
        totalIntervals = 4,
        startsWithFast = true,
        isCircuit = true
    )

    val all = listOf(formula2, formula3, formula4)
    val default = formula2
}
