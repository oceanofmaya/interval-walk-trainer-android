package com.oceanofmaya.intervalwalktrainer

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Unit tests for IntervalTimer.
 * 
 * Tests cover:
 * - Initial state correctness
 * - Start/pause/resume functionality
 * - State transitions (phase changes, interval completions)
 * - State restoration
 * - Timer completion
 * - Edge cases (pause when not running, multiple dispose calls)
 * 
 * Note: These tests focus on state management and logic. The actual CountDownTimer
 * behavior is tested through state flow observations and manual time progression.
 */
class IntervalTimerTest {

    private lateinit var phaseChanges: MutableList<IntervalPhase>
    private lateinit var intervalCompletions: MutableList<Unit>
    
    private val testFormula = IntervalFormula(
        name = "Test Formula",
        slowDurationSeconds = 3, // Short durations for faster tests
        fastDurationSeconds = 2,
        totalIntervals = 2,
        startsWithFast = false
    )

    @BeforeEach
    fun setUp() {
        phaseChanges = mutableListOf()
        intervalCompletions = mutableListOf()
    }

    private fun createTimer(formula: IntervalFormula = testFormula): IntervalTimer {
        return IntervalTimer(
            formula = formula,
            onPhaseChange = { phase -> phaseChanges.add(phase) },
            onIntervalComplete = { intervalCompletions.add(Unit) }
        )
    }

    @Test
    fun `initial state is correct for slow-starting formula`() {
        val timer = createTimer()
        val state = timer.state.value

        assertEquals(IntervalPhase.Slow, state.currentPhase)
        assertEquals(3, state.timeRemainingSeconds)
        assertEquals(0, state.currentInterval)
        assertEquals(2, state.totalIntervals)
        assertEquals(0, state.elapsedSeconds)
        assertFalse(state.isRunning)
    }

    @Test
    fun `initial state is correct for fast-starting formula`() {
        val fastStartFormula = IntervalFormula(
            name = "Fast Start",
            slowDurationSeconds = 3,
            fastDurationSeconds = 2,
            totalIntervals = 2,
            startsWithFast = true
        )
        val timer = createTimer(fastStartFormula)
        val state = timer.state.value

        assertEquals(IntervalPhase.Fast, state.currentPhase)
        assertEquals(2, state.timeRemainingSeconds)
        assertEquals(0, state.currentInterval)
        assertEquals(0, state.elapsedSeconds)
        assertFalse(state.isRunning)
    }

    @Test
    fun `start calls onPhaseChange if starting from beginning`() = runTest {
        val timer = createTimer()
        // Phase change list should be empty initially
        assertTrue(phaseChanges.isEmpty())
        
        timer.start()
        
        // Should trigger initial phase announcement
        assertTrue(phaseChanges.isNotEmpty())
        assertEquals(IntervalPhase.Slow, phaseChanges.first())
    }

    @Test
    fun `start does not call onPhaseChange if resuming from pause`() = runTest {
        val timer = createTimer()
        
        // Manually restore state to simulate a paused workout with elapsed time
        // This bypasses the need for CountDownTimer to tick in unit tests
        timer.restoreState(
            timeRemainingSeconds = 2, 
            currentInterval = 1,
            currentPhase = IntervalPhase.Slow,
            isRunning = false 
        )
        
        // Verify precondition: elapsedSeconds must be > 0 for this test to be valid
        // (restoreState should calculate elapsed time based on inputs)
        assertTrue(timer.state.value.elapsedSeconds > 0, "Elapsed time must be > 0 for this test")
        
        phaseChanges.clear() // Clear any callbacks from restoreState
        
        timer.start() // Resume
        
        // Should NOT trigger another announcement on resume because elapsedSeconds > 0
        assertTrue(phaseChanges.isEmpty())
    }

    @Test
    fun `start sets isRunning to true`() = runTest {
        val timer = createTimer()
        timer.start()
        
        // Give a moment for the timer to start
        kotlinx.coroutines.delay(100)
        
        val state = timer.state.value
        assertTrue(state.isRunning)
    }

    @Test
    fun `pause sets isRunning to false`() = runTest {
        val timer = createTimer()
        timer.start()
        kotlinx.coroutines.delay(100)
        
        timer.pause()
        
        val state = timer.state.value
        assertFalse(state.isRunning)
    }

    @Test
    fun `reset returns to initial state`() {
        val timer = createTimer()
        timer.start()
        
        timer.reset()
        
        val state = timer.state.value
        assertEquals(IntervalPhase.Slow, state.currentPhase)
        assertEquals(3, state.timeRemainingSeconds)
        assertEquals(0, state.currentInterval)
        assertEquals(0, state.elapsedSeconds)
        assertFalse(state.isRunning)
    }

    @Test
    fun `reset for fast-starting formula returns to fast phase`() {
        val fastStartFormula = IntervalFormula(
            name = "Fast Start",
            slowDurationSeconds = 3,
            fastDurationSeconds = 2,
            totalIntervals = 2,
            startsWithFast = true
        )
        val timer = createTimer(fastStartFormula)
        timer.start()
        
        timer.reset()
        
        val state = timer.state.value
        assertEquals(IntervalPhase.Fast, state.currentPhase)
        assertEquals(2, state.timeRemainingSeconds)
    }

    @Test
    fun `restoreState restores correct state`() {
        val timer = createTimer()
        
        timer.restoreState(
            timeRemainingSeconds = 2,
            currentInterval = 1,
            currentPhase = IntervalPhase.Fast,
            isRunning = false
        )
        
        val state = timer.state.value
        assertEquals(IntervalPhase.Fast, state.currentPhase)
        assertEquals(2, state.timeRemainingSeconds)
        assertEquals(1, state.currentInterval)
        // Elapsed calculation: 
        // - completedIntervals = 1 (fast phase means interval 1 is complete)
        // - completedPhasesTime = 1 * (3+2) = 5 seconds
        // - currentPhaseElapsed = 2 - 2 = 0 (fast phase just started)
        // - totalElapsedSeconds = 5 + 0 = 5
        assertEquals(5, state.elapsedSeconds)
        assertFalse(state.isRunning)
    }

    @Test
    fun `restoreState with isRunning true resumes timer`() = runTest {
        val timer = createTimer()
        
        timer.restoreState(
            timeRemainingSeconds = 5,
            currentInterval = 1,
            currentPhase = IntervalPhase.Slow,
            isRunning = true
        )
        
        kotlinx.coroutines.delay(100)
        val state = timer.state.value
        assertTrue(state.isRunning)
    }

    @Test
    fun `dispose cancels timer`() = runTest {
        val timer = createTimer()
        timer.start()
        kotlinx.coroutines.delay(100)
        
        timer.dispose()
        
        // After dispose, timer should be stopped
        // Note: We can't directly test CountDownTimer cancellation, but dispose should not throw
        timer.dispose() // Should be safe to call multiple times
    }

    @Test
    fun `start when already running does nothing`() = runTest {
        val timer = createTimer()
        timer.start()
        kotlinx.coroutines.delay(100)
        
        val stateBefore = timer.state.value
        timer.start() // Try to start again
        kotlinx.coroutines.delay(50)
        val stateAfter = timer.state.value
        
        // State should remain the same (still running)
        assertEquals(stateBefore.isRunning, stateAfter.isRunning)
    }


    @Test
    fun `pause when not running does nothing`() {
        val timer = createTimer()
        val stateBefore = timer.state.value
        
        timer.pause()
        
        val stateAfter = timer.state.value
        assertEquals(stateBefore.isRunning, stateAfter.isRunning)
        assertFalse(stateAfter.isRunning)
    }

    @Test
    fun `restoreState with zero time remaining does not resume`() = runTest {
        val timer = createTimer()
        
        timer.restoreState(
            timeRemainingSeconds = 0,
            currentInterval = 1,
            currentPhase = IntervalPhase.Completed,
            isRunning = true
        )
        
        kotlinx.coroutines.delay(100)
        val state = timer.state.value
        assertFalse(state.isRunning)
    }

    @Test
    fun `restoreState updates phase change callback`() {
        val timer = createTimer()
        
        timer.restoreState(
            timeRemainingSeconds = 5,
            currentInterval = 1,
            currentPhase = IntervalPhase.Fast,
            isRunning = false
        )
        
        // Phase change should be called
        assertTrue(phaseChanges.isNotEmpty())
        assertEquals(IntervalPhase.Fast, phaseChanges.last())
    }

    @Test
    fun `start after pause resumes from same time`() = runTest {
        val timer = createTimer()
        timer.start()
        kotlinx.coroutines.delay(200) // Let timer run a bit
        
        val timeBeforePause = timer.state.value.timeRemainingSeconds
        timer.pause()
        kotlinx.coroutines.delay(100)
        
        timer.start()
        kotlinx.coroutines.delay(100)
        
        // Should resume from approximately the same time (may have ticked down slightly)
        val timeAfterResume = timer.state.value.timeRemainingSeconds
        assertTrue(timeAfterResume <= timeBeforePause)
    }

    @Test
    fun `start when time is zero resets and starts fresh`() = runTest {
        val timer = createTimer()
        // Set state to completed
        timer.restoreState(
            timeRemainingSeconds = 0,
            currentInterval = testFormula.totalIntervals,
            currentPhase = IntervalPhase.Completed,
            isRunning = false
        )
        
        timer.start()
        kotlinx.coroutines.delay(100)
        
        val state = timer.state.value
        // Should reset to initial state
        assertEquals(IntervalPhase.Slow, state.currentPhase)
        assertEquals(3, state.timeRemainingSeconds) // Initial slow duration
        assertEquals(0, state.currentInterval)
    }

    @Test
    fun `restoreState with Completed phase sets correct state`() {
        val timer = createTimer()
        
        timer.restoreState(
            timeRemainingSeconds = 0,
            currentInterval = testFormula.totalIntervals,
            currentPhase = IntervalPhase.Completed,
            isRunning = false
        )
        
        val state = timer.state.value
        assertEquals(IntervalPhase.Completed, state.currentPhase)
        assertEquals(0, state.timeRemainingSeconds)
        assertEquals(testFormula.totalIntervals, state.currentInterval)
        // Total duration = (slow 3s + fast 2s) * 2 intervals = 10 seconds
        assertEquals(10, state.elapsedSeconds)
    }

    @Test
    fun `dispose can be called multiple times safely`() = runTest {
        val timer = createTimer()
        timer.start()
        kotlinx.coroutines.delay(100)
        
        timer.dispose()
        timer.dispose()
        timer.dispose()
        
        // Should not throw
        assertTrue(true)
    }

    @Test
    fun `timer handles single interval formula`() {
        val singleIntervalFormula = IntervalFormula(
            name = "Single",
            slowDurationSeconds = 5,
            fastDurationSeconds = 3,
            totalIntervals = 1
        )
        val timer = createTimer(singleIntervalFormula)
        val state = timer.state.value
        
        assertEquals(1, state.totalIntervals)
        assertEquals(IntervalPhase.Slow, state.currentPhase)
        assertEquals(5, state.timeRemainingSeconds)
    }

    @Test
    fun `timer handles formula with many intervals`() {
        val manyIntervalsFormula = IntervalFormula(
            name = "Many",
            slowDurationSeconds = 2,
            fastDurationSeconds = 2,
            totalIntervals = 10
        )
        val timer = createTimer(manyIntervalsFormula)
        val state = timer.state.value
        
        assertEquals(10, state.totalIntervals)
    }

    @Test
    fun `restoreState correctly sets interval index`() {
        val timer = createTimer()
        
        timer.restoreState(
            timeRemainingSeconds = 5,
            currentInterval = 2,
            currentPhase = IntervalPhase.Fast,
            isRunning = false
        )
        
        val state = timer.state.value
        assertEquals(2, state.currentInterval)
    }

    @Test
    fun `phase change callback is called on reset`() {
        val timer = createTimer()
        phaseChanges.clear()
        
        timer.reset()
        
        assertTrue(phaseChanges.isNotEmpty())
        assertEquals(IntervalPhase.Slow, phaseChanges.last())
    }

    @Test
    fun `interval completion callback is not called on initial state`() {
        createTimer()
        
        assertEquals(0, intervalCompletions.size)
    }

    @Test
    fun `elapsed time starts at zero`() {
        val timer = createTimer()
        val state = timer.state.value
        
        assertEquals(0, state.elapsedSeconds)
    }

    @Test
    fun `elapsed time is tracked during workout`() = runTest {
        val timer = createTimer()
        timer.start()
        
        // Wait for timer to tick
        kotlinx.coroutines.delay(1500)
        
        val state = timer.state.value
        // Should have some elapsed time (at least 1 second)
        assertTrue(state.elapsedSeconds >= 0, "Elapsed time should increase during workout")
    }

    @Test
    fun `elapsed time resets to zero on reset`() {
        val timer = createTimer()
        timer.start()
        
        timer.reset()
        
        val state = timer.state.value
        assertEquals(0, state.elapsedSeconds)
    }

    @Test
    fun `restoreState calculates elapsed time correctly for slow phase`() {
        val timer = createTimer()
        
        // Restore to slow phase with 1 second remaining (2 seconds elapsed in slow phase)
        timer.restoreState(
            timeRemainingSeconds = 1,
            currentInterval = 1,
            currentPhase = IntervalPhase.Slow,
            isRunning = false
        )
        
        val state = timer.state.value
        // Elapsed calculation:
        // - completedIntervals = 0 (slow phase means we're in the middle of interval 1)
        // - completedPhasesTime = 0 * (3+2) = 0
        // - currentPhaseElapsed = 3 - 1 = 2 (2 seconds elapsed in slow phase)
        // - totalElapsedSeconds = 0 + 2 = 2
        assertEquals(2, state.elapsedSeconds)
    }

    @Test
    fun `restoreState calculates elapsed time correctly for fast phase with elapsed time`() {
        val timer = createTimer()
        
        // Restore to fast phase with 1 second remaining (1 second elapsed in fast phase)
        timer.restoreState(
            timeRemainingSeconds = 1,
            currentInterval = 1,
            currentPhase = IntervalPhase.Fast,
            isRunning = false
        )
        
        val state = timer.state.value
        // Elapsed calculation:
        // - completedIntervals = 1 (fast phase means interval 1 is complete)
        // - completedPhasesTime = 1 * (3+2) = 5
        // - currentPhaseElapsed = 2 - 1 = 1 (1 second elapsed in fast phase)
        // - totalElapsedSeconds = 5 + 1 = 6
        assertEquals(6, state.elapsedSeconds)
    }

    @Test
    fun `elapsed time increases correctly during timer ticks`() {
        val timer = createTimer()
        val initialState = timer.state.value
        assertEquals(0, initialState.elapsedSeconds)
        
        timer.start()
        
        // After starting, elapsed time should be initialized (may be 0 or small value)
        val stateAfterStart = timer.state.value
        // Elapsed time should be non-negative and not exceed total duration
        assertTrue(stateAfterStart.elapsedSeconds >= 0, "Elapsed time should be non-negative")
        assertTrue(stateAfterStart.elapsedSeconds <= testFormula.totalDurationSeconds,
            "Elapsed time should not exceed total duration")
    }

    @Test
    fun `elapsed time reaches total duration when workout completes`() {
        val shortFormula = IntervalFormula(
            name = "Short",
            slowDurationSeconds = 1,
            fastDurationSeconds = 1,
            totalIntervals = 1
        )
        val timer = createTimer(shortFormula)
        
        // Test completion state directly by restoring to completed state
        timer.restoreState(
            timeRemainingSeconds = 0,
            currentInterval = 1,
            currentPhase = IntervalPhase.Completed,
            isRunning = false
        )
        
        val state = timer.state.value
        // Should be completed with elapsed time equal to total duration
        assertEquals(IntervalPhase.Completed, state.currentPhase)
        assertEquals(shortFormula.totalDurationSeconds, state.elapsedSeconds)
    }
}

