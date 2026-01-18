package com.oceanofmaya.intervalwalktrainer

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for individual workout sessions.
 */
@Dao
interface WorkoutSessionDao {
    
    /**
     * Get all workout sessions, ordered by date descending, then by timestamp descending.
     */
    @Query("SELECT * FROM workout_sessions ORDER BY date DESC, timestamp DESC")
    fun getAllSessions(): Flow<List<WorkoutSession>>
    
    /**
     * Get workout sessions for a specific date, ordered by timestamp descending.
     */
    @Query("SELECT * FROM workout_sessions WHERE date = :date ORDER BY timestamp DESC")
    suspend fun getSessionsByDate(date: String): List<WorkoutSession>
    
    /**
     * Get workout sessions for a date range, ordered by date descending, then by timestamp descending.
     */
    @Query("SELECT * FROM workout_sessions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC, timestamp DESC")
    suspend fun getSessionsByDateRange(startDate: String, endDate: String): List<WorkoutSession>
    
    /**
     * Insert a new workout session.
     */
    @Insert
    suspend fun insert(session: WorkoutSession)
    
    /**
     * Delete a workout session by ID.
     */
    @Query("DELETE FROM workout_sessions WHERE id = :id")
    suspend fun deleteById(id: Long)
    
    /**
     * Delete all workout sessions for a specific date.
     */
    @Query("DELETE FROM workout_sessions WHERE date = :date")
    suspend fun deleteByDate(date: String)
    
    /**
     * Delete all workout sessions.
     */
    @Query("DELETE FROM workout_sessions")
    suspend fun deleteAll()
}

