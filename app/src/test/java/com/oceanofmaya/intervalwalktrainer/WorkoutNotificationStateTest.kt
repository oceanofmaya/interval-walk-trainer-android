package com.oceanofmaya.intervalwalktrainer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WorkoutNotificationStateTest {

    @Test
    fun `circuit timer state maps interval pair to round`() {
        val notificationState = WorkoutNotificationState.fromTimer(
            state = timerState(currentInterval = 4, totalIntervals = 6),
            isCircuit = true
        )

        assertEquals(2, notificationState?.currentPosition)
        assertEquals(3, notificationState?.totalPositions)
        assertTrue(notificationState?.isCircuit == true)
    }

    @Test
    fun `standard timer state preserves round position`() {
        val notificationState = WorkoutNotificationState.fromTimer(
            state = timerState(currentInterval = 3, totalIntervals = 5),
            isCircuit = false
        )

        assertEquals(3, notificationState?.currentPosition)
        assertEquals(5, notificationState?.totalPositions)
        assertFalse(notificationState?.isCircuit == true)
    }

    @Test
    fun `completed timer does not produce ongoing notification state`() {
        val notificationState = WorkoutNotificationState.fromTimer(
            state = timerState(currentPhase = IntervalPhase.Completed),
            isCircuit = false
        )

        assertNull(notificationState)
    }

    @Test
    fun `running countdown ticks do not replace chronometer notification`() {
        val previous = notificationState(remainingSeconds = 30, isRunning = true)
        val next = previous.copy(remainingSeconds = 29)

        assertFalse(next.shouldReplace(previous))
    }

    @Test
    fun `phase progress and control changes replace notification`() {
        val previous = notificationState(remainingSeconds = 30, isRunning = true)

        assertTrue(previous.copy(phase = WorkoutNotificationPhase.FAST).shouldReplace(previous))
        assertTrue(previous.copy(currentPosition = 2).shouldReplace(previous))
        assertTrue(previous.copy(isRunning = false).shouldReplace(previous))
    }

    @Test
    fun `paused state exposes resume and preserves frozen countdown updates`() {
        val previous = notificationState(remainingSeconds = 30, isRunning = false)
        val next = previous.copy(remainingSeconds = 29)

        assertEquals(WorkoutNotificationAction.RESUME, previous.action)
        assertTrue(next.shouldReplace(previous))
    }

    private fun timerState(
        currentPhase: IntervalPhase = IntervalPhase.Slow,
        currentInterval: Int = 1,
        totalIntervals: Int = 5
    ) = TimerState(
        currentPhase = currentPhase,
        timeRemainingSeconds = 30,
        currentInterval = currentInterval,
        totalIntervals = totalIntervals,
        isRunning = true,
        elapsedSeconds = 30
    )

    private fun notificationState(
        remainingSeconds: Int,
        isRunning: Boolean
    ) = WorkoutNotificationState(
        phase = WorkoutNotificationPhase.SLOW,
        remainingSeconds = remainingSeconds,
        currentPosition = 1,
        totalPositions = 5,
        isCircuit = false,
        isRunning = isRunning
    )
}
