package com.oceanofmaya.intervalwalktrainer

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a workout record for a specific date.
 * 
 * @param date The date of the workout in YYYY-MM-DD format
 * @param completedWorkouts Number of workouts completed on this date
 * @param totalMinutes Total minutes walked on this date
 * @param lastWorkoutTimestamp Timestamp of the last workout completion on this date
 */
@Entity(tableName = "workout_records")
data class WorkoutRecord(
    @PrimaryKey
    val date: String, // Format: YYYY-MM-DD
    val completedWorkouts: Int = 1,
    val totalMinutes: Int = 0,
    val lastWorkoutTimestamp: Long = System.currentTimeMillis()
)
