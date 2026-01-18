package com.oceanofmaya.intervalwalktrainer

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents an individual workout session with its type and duration.
 * 
 * @param id Auto-generated primary key
 * @param date The date of the workout in YYYY-MM-DD format
 * @param workoutType The name/type of the workout formula used
 * @param minutes The duration of this workout in minutes
 * @param timestamp Timestamp when the workout was completed
 */
@Entity(tableName = "workout_sessions")
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String, // Format: YYYY-MM-DD
    val workoutType: String, // Formula name (e.g., "3-3 Japanese - 5 Rounds (30 min)")
    val minutes: Int,
    val timestamp: Long = System.currentTimeMillis()
)

