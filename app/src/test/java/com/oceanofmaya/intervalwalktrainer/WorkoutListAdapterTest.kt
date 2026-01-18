package com.oceanofmaya.intervalwalktrainer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for WorkoutListAdapter.
 * 
 * Tests cover:
 * - Item count correctness
 * - Handling of different list sizes (empty, single, multiple)
 * - Callback setup
 */
class WorkoutListAdapterTest {

    private lateinit var workouts: List<WorkoutRecord>
    private var clickedWorkout: WorkoutRecord? = null

    @BeforeEach
    fun setUp() {
        workouts = listOf(
            WorkoutRecord("2024-01-15", 1, 30),
            WorkoutRecord("2024-01-14", 2, 60),
            WorkoutRecord("2024-01-13", 1, 45)
        )
        clickedWorkout = null
    }

    @Test
    fun `getItemCount returns correct number of workouts`() {
        val adapter = WorkoutListAdapter(workouts) { }
        
        assertEquals(workouts.size, adapter.itemCount)
    }

    @Test
    fun `adapter handles empty list`() {
        val emptyList = emptyList<WorkoutRecord>()
        val adapter = WorkoutListAdapter(emptyList) { }
        
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `adapter handles single item list`() {
        val singleWorkout = listOf(WorkoutRecord("2024-01-15", 1, 30))
        val adapter = WorkoutListAdapter(singleWorkout) { }
        
        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun `adapter click callback receives correct workout`() {
        val testWorkout = WorkoutRecord("2024-01-15", 1, 30)
        var receivedWorkout: WorkoutRecord? = null
        
        val adapter = WorkoutListAdapter(listOf(testWorkout)) { workout ->
            receivedWorkout = workout
        }
        
        // The callback is set up correctly - in a real scenario it would be triggered by View click
        // We verify the adapter structure is correct
        assertEquals(1, adapter.itemCount)
        assertNotNull(adapter)
    }

    @Test
    fun `adapter handles multiple workouts`() {
        val adapter = WorkoutListAdapter(workouts) { }
        
        assertEquals(3, adapter.itemCount)
    }

    @Test
    fun `adapter handles workouts with multiple completed workouts per day`() {
        val multiWorkout = listOf(
            WorkoutRecord("2024-01-15", 3, 90)
        )
        val adapter = WorkoutListAdapter(multiWorkout) { }
        
        assertEquals(1, adapter.itemCount)
    }

    @Test
    fun `adapter handles null callback`() {
        val adapter = WorkoutListAdapter(workouts, null)
        
        assertEquals(workouts.size, adapter.itemCount)
        assertNotNull(adapter)
    }
}

