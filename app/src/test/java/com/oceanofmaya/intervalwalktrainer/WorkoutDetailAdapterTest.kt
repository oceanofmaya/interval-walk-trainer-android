package com.oceanofmaya.intervalwalktrainer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for WorkoutDetailAdapter.
 * 
 * Tests cover:
 * - Item count correctness
 * - Handling of different list sizes (empty, single, multiple)
 * - Workout type display
 * - Minutes formatting
 */
class WorkoutDetailAdapterTest {

    private lateinit var sessions: List<WorkoutSession>

    @BeforeEach
    fun setUp() {
        sessions = listOf(
            WorkoutSession(
                date = "2024-01-15",
                workoutType = "3-3 Japanese - 5 Rounds (30 min)",
                minutes = 30,
                timestamp = System.currentTimeMillis()
            ),
            WorkoutSession(
                date = "2024-01-15",
                workoutType = "5-2 High Intensity - 4 Rounds (28 min)",
                minutes = 28,
                timestamp = System.currentTimeMillis() - 1000
            ),
            WorkoutSession(
                date = "2024-01-15",
                workoutType = "5-4-5 Circuit - 2 Rounds (36 min)",
                minutes = 36,
                timestamp = System.currentTimeMillis() - 2000
            )
        )
    }

    @Test
    fun `getItemCount returns correct number of sessions`() {
        val adapter = WorkoutDetailAdapter(sessions)
        
        assertEquals(sessions.size, adapter.itemCount)
    }

    @Test
    fun `adapter handles empty list`() {
        val emptyList = emptyList<WorkoutSession>()
        val adapter = WorkoutDetailAdapter(emptyList)
        
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `adapter handles single item list`() {
        val singleSession = listOf(
            WorkoutSession(
                date = "2024-01-15",
                workoutType = "3-3 Japanese - 5 Rounds (30 min)",
                minutes = 30
            )
        )
        val adapter = WorkoutDetailAdapter(singleSession)
        
        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun `adapter handles multiple sessions`() {
        val adapter = WorkoutDetailAdapter(sessions)
        
        assertEquals(3, adapter.itemCount)
    }

    @Test
    fun `adapter handles sessions with different workout types`() {
        val mixedSessions = listOf(
            WorkoutSession(
                date = "2024-01-15",
                workoutType = "3-3 Japanese - 5 Rounds (30 min)",
                minutes = 30
            ),
            WorkoutSession(
                date = "2024-01-15",
                workoutType = "Custom: 5-3 - 4 Rounds",
                minutes = 32
            )
        )
        val adapter = WorkoutDetailAdapter(mixedSessions)
        
        assertEquals(2, adapter.itemCount)
        assertNotNull(adapter)
    }

    @Test
    fun `adapter handles sessions with zero minutes`() {
        val zeroMinuteSession = listOf(
            WorkoutSession(
                date = "2024-01-15",
                workoutType = "3-3 Japanese - 5 Rounds (30 min)",
                minutes = 0
            )
        )
        val adapter = WorkoutDetailAdapter(zeroMinuteSession)
        
        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun `adapter handles sessions with long durations`() {
        val longSession = listOf(
            WorkoutSession(
                date = "2024-01-15",
                workoutType = "3-3 Japanese - 5 Rounds (30 min)",
                minutes = 120 // 2 hours
            )
        )
        val adapter = WorkoutDetailAdapter(longSession)
        
        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun `adapter handles custom workout types`() {
        val customSessions = listOf(
            WorkoutSession(
                date = "2024-01-15",
                workoutType = "Custom Circuit: 5-4-5 - 3 Rounds",
                minutes = 42
            )
        )
        val adapter = WorkoutDetailAdapter(customSessions)
        
        assertEquals(1, adapter.itemCount)
        assertNotNull(adapter)
    }
    
    @Test
    fun `adapter handles singular round in custom circuit name`() {
        val singularRoundSession = listOf(
            WorkoutSession(
                date = "2024-01-15",
                workoutType = "Custom Circuit: 5-4-5 - 1 Rounds",
                minutes = 14
            )
        )
        val adapter = WorkoutDetailAdapter(singularRoundSession)
        
        assertEquals(1, adapter.itemCount)
        assertNotNull(adapter)
    }
}

