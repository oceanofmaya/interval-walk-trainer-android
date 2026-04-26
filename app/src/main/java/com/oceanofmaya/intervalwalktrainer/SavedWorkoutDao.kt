package com.oceanofmaya.intervalwalktrainer

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SavedWorkoutDao {

    @Query("SELECT * FROM saved_workouts ORDER BY sortOrder ASC, createdAt ASC")
    fun observeAllOrdered(): Flow<List<SavedWorkout>>

    @Query("SELECT * FROM saved_workouts ORDER BY sortOrder ASC, createdAt ASC")
    suspend fun getAllOrdered(): List<SavedWorkout>

    @Query("SELECT * FROM saved_workouts WHERE id = :id")
    suspend fun getById(id: Long): SavedWorkout?

    @Query("SELECT COUNT(*) FROM saved_workouts")
    suspend fun count(): Int

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM saved_workouts")
    suspend fun maxSortOrder(): Int

    /** Returns the first row whose canonical signature (durations + intervals + circuit shape) matches, or null. */
    @Query(
        """
        SELECT * FROM saved_workouts
        WHERE slowDurationSeconds = :slow
          AND fastDurationSeconds = :fast
          AND totalIntervals = :intervals
          AND isCircuit = :isCircuit
          AND circuitPattern = :circuitPattern
          AND startsWithFast = :startsWithFast
        LIMIT 1
        """
    )
    suspend fun findBySignature(
        slow: Int,
        fast: Int,
        intervals: Int,
        isCircuit: Boolean,
        circuitPattern: String,
        startsWithFast: Boolean
    ): SavedWorkout?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(workout: SavedWorkout): Long

    /**
     * Re-inserts a previously-deleted row preserving its primary key (and by extension its
     * sortOrder + createdAt). Used by [SavedWorkoutRepository.restore] to back the undo-delete
     * snackbar. REPLACE semantics mean that in the rare case the id has been reused by a new
     * insert during the undo window, the new row is overwritten — acceptable because
     * autoGenerate ids are monotonic and collisions are vanishingly unlikely in practice.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(workout: SavedWorkout): Long

    @Update
    suspend fun update(workout: SavedWorkout)

    @Query("DELETE FROM saved_workouts WHERE id = :id")
    suspend fun deleteById(id: Long)
}
