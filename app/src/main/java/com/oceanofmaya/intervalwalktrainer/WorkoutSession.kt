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
 * @param stepCount Optional step count captured during active workout intervals
 * @param averageHeartRateBpm Optional average heart rate for active workout intervals
 * @param minHeartRateBpm Optional minimum heart rate for active workout intervals
 * @param maxHeartRateBpm Optional maximum heart rate for active workout intervals
 * @param stepSource Optional source label for the saved step count
 * @param heartRateSource Optional source label for the saved heart-rate summary
 * @param startedAt Optional timestamp when the workout session started
 * @param metricsIntervalsJson Optional encoded active workout intervals used for Health Connect reads
 * @param formulaSnapshotJson Optional encoded formula used to rebuild phase windows
 * @param metricsPhaseWindowsJson Optional encoded slow/fast phase windows for Health Connect reads
 * @param fastPhaseAverageHeartRateBpm Optional average heart rate across fast phases
 * @param slowPhaseAverageHeartRateBpm Optional average heart rate across slow phases
 */
@Entity(tableName = "workout_sessions")
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String, // Format: YYYY-MM-DD
    val workoutType: String, // Formula name (e.g., "3-3 Japanese - 5 Rounds")
    val minutes: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val stepCount: Int? = null,
    val averageHeartRateBpm: Int? = null,
    val minHeartRateBpm: Int? = null,
    val maxHeartRateBpm: Int? = null,
    val stepSource: String? = null,
    val heartRateSource: String? = null,
    val startedAt: Long? = null,
    val metricsIntervalsJson: String? = null,
    val formulaSnapshotJson: String? = null,
    val metricsPhaseWindowsJson: String? = null,
    val fastPhaseAverageHeartRateBpm: Int? = null,
    val slowPhaseAverageHeartRateBpm: Int? = null
)

