package com.oceanofmaya.intervalwalktrainer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * Unit tests for WorkoutSession data class.
 * 
 * Tests cover:
 * - Default values
 * - Data class properties
 * - Timestamp generation
 */
class WorkoutSessionTest {
    
    @Test
    fun `WorkoutSession has correct default values`() {
        val session = WorkoutSession(
            date = "2024-01-01",
            workoutType = "3-3 Japanese - 5 Rounds (30 min)",
            minutes = 30
        )
        
        assertEquals("2024-01-01", session.date)
        assertEquals("3-3 Japanese - 5 Rounds (30 min)", session.workoutType)
        assertEquals(30, session.minutes)
        assertEquals(0L, session.id) // Auto-generated, defaults to 0
        assert(session.timestamp > 0)
    }
    
    @Test
    fun `WorkoutSession can be created with all values`() {
        val timestamp = System.currentTimeMillis()
        val session = WorkoutSession(
            id = 1L,
            date = "2024-01-01",
            workoutType = "5-2 High Intensity - 4 Rounds (28 min)",
            minutes = 28,
            timestamp = timestamp
        )
        
        assertEquals(1L, session.id)
        assertEquals("2024-01-01", session.date)
        assertEquals("5-2 High Intensity - 4 Rounds (28 min)", session.workoutType)
        assertEquals(28, session.minutes)
        assertEquals(timestamp, session.timestamp)
    }
    
    @Test
    fun `WorkoutSession can be copied`() {
        val original = WorkoutSession(
            date = "2024-01-01",
            workoutType = "3-3 Japanese - 5 Rounds (30 min)",
            minutes = 30
        )
        
        val updated = original.copy(
            workoutType = "5-4-5 Circuit - 2 Rounds (36 min)",
            minutes = 36
        )
        
        assertEquals(original.id, updated.id)
        assertEquals(original.date, updated.date)
        assertEquals("5-4-5 Circuit - 2 Rounds (36 min)", updated.workoutType)
        assertEquals(36, updated.minutes)
        assertEquals(original.timestamp, updated.timestamp)
    }
    
    @Test
    fun `WorkoutSession handles custom workout types`() {
        val session = WorkoutSession(
            date = "2024-01-01",
            workoutType = "Custom: 5-3 - 4 Rounds",
            minutes = 32
        )
        
        assertEquals("Custom: 5-3 - 4 Rounds", session.workoutType)
        assertEquals(32, session.minutes)
    }
}

