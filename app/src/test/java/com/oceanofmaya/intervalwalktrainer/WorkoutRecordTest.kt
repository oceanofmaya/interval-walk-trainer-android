package com.oceanofmaya.intervalwalktrainer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for WorkoutRecord data class.
 * 
 * Tests cover:
 * - Default values
 * - Data class properties
 */
class WorkoutRecordTest {
    
    @Test
    fun `WorkoutRecord has correct default values`() {
        val record = WorkoutRecord(
            date = "2024-01-01"
        )
        
        assertEquals("2024-01-01", record.date)
        assertEquals(1, record.completedWorkouts)
        assertEquals(0, record.totalMinutes)
        assertTrue(record.lastWorkoutTimestamp > 0)
    }
    
    @Test
    fun `WorkoutRecord can be created with all values`() {
        val timestamp = System.currentTimeMillis()
        val record = WorkoutRecord(
            date = "2024-01-01",
            completedWorkouts = 3,
            totalMinutes = 90,
            lastWorkoutTimestamp = timestamp
        )
        
        assertEquals("2024-01-01", record.date)
        assertEquals(3, record.completedWorkouts)
        assertEquals(90, record.totalMinutes)
        assertEquals(timestamp, record.lastWorkoutTimestamp)
    }
    
    @Test
    fun `WorkoutRecord can be copied`() {
        val original = WorkoutRecord(
            date = "2024-01-01",
            completedWorkouts = 2,
            totalMinutes = 60
        )
        
        val updated = original.copy(
            completedWorkouts = 3,
            totalMinutes = 90
        )
        
        assertEquals("2024-01-01", updated.date)
        assertEquals(3, updated.completedWorkouts)
        assertEquals(90, updated.totalMinutes)
        assertEquals(original.lastWorkoutTimestamp, updated.lastWorkoutTimestamp)
    }
}

