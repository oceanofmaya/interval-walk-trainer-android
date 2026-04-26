package com.oceanofmaya.intervalwalktrainer

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * User-saved custom workout template (library row). Canonical fields mirror [IntervalFormula]
 * plus [circuitPattern] for circuit mode.
 */
@Entity(tableName = "saved_workouts")
data class SavedWorkout(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val displayName: String,
    val slowDurationSeconds: Int,
    val fastDurationSeconds: Int,
    val totalIntervals: Int,
    val isCircuit: Boolean,
    /** [SavedWorkoutRepository.CIRCUIT_PATTERN_FAST_SLOW_FAST] or SLOW_FAST_SLOW when [isCircuit]. */
    val circuitPattern: String,
    val startsWithFast: Boolean,
    val createdAt: Long = System.currentTimeMillis(),
    val sortOrder: Int = 0
) {
    fun toIntervalFormula(): IntervalFormula = IntervalFormula(
        name = displayName,
        slowDurationSeconds = slowDurationSeconds,
        fastDurationSeconds = fastDurationSeconds,
        totalIntervals = totalIntervals,
        startsWithFast = startsWithFast,
        isCircuit = isCircuit
    )
}
