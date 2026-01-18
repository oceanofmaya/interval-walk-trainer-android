package com.oceanofmaya.intervalwalktrainer

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for workout records.
 */
@Dao
interface WorkoutDao {
    
    /**
     * Get all workout records, ordered by date descending.
     */
    @Query("SELECT * FROM workout_records ORDER BY date DESC")
    fun getAllRecords(): Flow<List<WorkoutRecord>>
    
    /**
     * Get a specific workout record by date.
     */
    @Query("SELECT * FROM workout_records WHERE date = :date")
    suspend fun getRecordByDate(date: String): WorkoutRecord?
    
    /**
     * Get workout records for a date range.
     */
    @Query("SELECT * FROM workout_records WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    suspend fun getRecordsByDateRange(startDate: String, endDate: String): List<WorkoutRecord>
    
    /**
     * Get all workout records as a list (non-flow).
     */
    @Query("SELECT * FROM workout_records ORDER BY date DESC")
    suspend fun getAllRecordsList(): List<WorkoutRecord>
    
    /**
     * Insert or update a workout record.
     * If a record for the date exists, it will be updated.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(record: WorkoutRecord)
    
    /**
     * Delete a workout record by date.
     */
    @Query("DELETE FROM workout_records WHERE date = :date")
    suspend fun deleteByDate(date: String)
    
    /**
     * Get total number of workouts across all dates.
     */
    @Query("SELECT SUM(completedWorkouts) FROM workout_records")
    suspend fun getTotalWorkouts(): Int?
    
    /**
     * Get total minutes walked across all dates.
     */
    @Query("SELECT SUM(totalMinutes) FROM workout_records")
    suspend fun getTotalMinutes(): Int?
    
    /**
     * Get the earliest workout date.
     */
    @Query("SELECT MIN(date) FROM workout_records")
    suspend fun getEarliestDate(): String?
    
    /**
     * Get the latest workout date.
     */
    @Query("SELECT MAX(date) FROM workout_records")
    suspend fun getLatestDate(): String?
    
    /**
     * Delete all workout records.
     */
    @Query("DELETE FROM workout_records")
    suspend fun deleteAll()
}
