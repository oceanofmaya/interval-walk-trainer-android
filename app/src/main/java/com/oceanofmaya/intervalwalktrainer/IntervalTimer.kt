package com.oceanofmaya.intervalwalktrainer

import android.os.CountDownTimer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Represents the current phase of an interval training session.
 */
sealed class IntervalPhase {
    /** Slow walking/recovery phase */
    object Slow : IntervalPhase()
    
    /** Fast walking/high-intensity phase */
    object Fast : IntervalPhase()
    
    /** Workout completed */
    object Completed : IntervalPhase()
}

/**
 * Represents the current state of the interval timer.
 * 
 * @param currentPhase The current training phase (slow, fast, or completed)
 * @param timeRemainingSeconds Remaining time in the current phase
 * @param currentInterval Current interval number (1-indexed)
 * @param totalIntervals Total number of intervals in the workout
 * @param isRunning Whether the timer is currently running
 * @param elapsedSeconds Total elapsed time in the workout (in seconds)
 */
data class TimerState(
    val currentPhase: IntervalPhase = IntervalPhase.Slow,
    val timeRemainingSeconds: Int = 0,
    val currentInterval: Int = 0,
    val totalIntervals: Int = 0,
    val isRunning: Boolean = false,
    val elapsedSeconds: Int = 0
)

/**
 * Manages the interval training timer logic.
 * 
 * Handles phase transitions, time counting, and state management.
 * Uses early notification triggers (2.5 seconds) to allow TTS to complete
 * before phase transitions for better user experience.
 * 
 * @param formula The training formula to execute
 * @param onPhaseChange Callback invoked when phase changes (called early for TTS alignment)
 * @param onIntervalComplete Callback invoked when an interval completes
 */
class IntervalTimer(
    private val formula: IntervalFormula,
    private val onPhaseChange: (IntervalPhase) -> Unit,
    private val onIntervalComplete: () -> Unit
) {
    private var countDownTimer: CountDownTimer? = null
    private val initialPhase = if (formula.startsWithFast) IntervalPhase.Fast else IntervalPhase.Slow
    private val initialTime = if (formula.startsWithFast) formula.fastDurationSeconds else formula.slowDurationSeconds
    private val _state = MutableStateFlow(
        TimerState(
            currentPhase = initialPhase,
            totalIntervals = formula.totalIntervals,
            timeRemainingSeconds = initialTime,
            elapsedSeconds = 0
        )
    )
    val state: StateFlow<TimerState> = _state.asStateFlow()

    private var currentIntervalIndex = 0
    private var isSlowPhase = !formula.startsWithFast
    private var totalElapsedSeconds = 0
    private var phaseStartTime = 0

    /**
     * For fast-start formulas (interval mode only), the workout ends on a final slow phase.
     * Circuits end after the last fast phase, so this is false when isCircuit.
     */
    private fun isFinalSlowPhaseForFastStart(): Boolean {
        if (formula.isCircuit) return false
        return formula.startsWithFast && isSlowPhase && currentIntervalIndex >= formula.totalIntervals
    }

    /**
     * Determines the phase that should follow when the current fast phase finishes.
     * For circuit: complete after last fast; at end of a round (even currentIntervalIndex) start next round with Fast.
     * For interval fast-start: last fast transitions to trailing slow; otherwise to Slow.
     */
    private fun nextPhaseAfterFastCompletes(): IntervalPhase {
        val nextIndex = currentIntervalIndex + 1
        if (nextIndex >= formula.totalIntervals) {
            // Last fast: circuit or slow-start interval completes here; fast-start interval goes to trailing slow
            return if (formula.isCircuit || !formula.startsWithFast) IntervalPhase.Completed else IntervalPhase.Slow
        }
        return if (formula.isCircuit && nextIndex % 2 == 0) {
            // Just finished second fast of a round → start next round with Fast
            IntervalPhase.Fast
        } else {
            IntervalPhase.Slow
        }
    }

    fun start() {
        if (_state.value.isRunning) return

        val currentTime = _state.value.timeRemainingSeconds
        if (currentTime > 0) {
            // Trigger initial phase announcement if starting from beginning
            if (_state.value.elapsedSeconds == 0) {
                onPhaseChange(_state.value.currentPhase)
            }
            resume(currentTime)
        } else {
            // Reset and start fresh
            countDownTimer?.cancel()
            currentIntervalIndex = 0
            isSlowPhase = !formula.startsWithFast
            val startPhase = if (formula.startsWithFast) IntervalPhase.Fast else IntervalPhase.Slow
            val startTime = if (formula.startsWithFast) formula.fastDurationSeconds else formula.slowDurationSeconds
            totalElapsedSeconds = 0
            phaseStartTime = startTime
            isSlowPhase = !formula.startsWithFast
            _state.value = TimerState(
                currentPhase = startPhase,
                timeRemainingSeconds = startTime,
                currentInterval = 0,
                totalIntervals = formula.totalIntervals,
                isRunning = false,
                elapsedSeconds = 0
            )
            onPhaseChange(startPhase)
            resume(startTime)
        }
    }

    private fun resume(seconds: Int) {
        _state.value = _state.value.copy(isRunning = true)
        startTimer(seconds)
    }

    /**
     * Starts a countdown timer for the specified duration.
     * 
     * Important: Implements early phase change notifications to improve TTS timing.
     * TTS (Text-to-Speech) takes time to speak the phase change message. To ensure
     * the spoken message completes right as the timer transitions, we trigger the
     * notification 2.5 seconds early (or 1 second for very short timers).
     * 
     * Example: For a 3-minute phase:
     * - At 2.5s remaining: TTS speaks "Fast walk" (takes ~2s to speak)
     * - At 0s: Timer actually transitions, and TTS has just finished speaking
     * 
     * This creates seamless synchronization between the spoken notification and
     * the actual phase transition.
     */
    private fun startTimer(seconds: Int) {
        countDownTimer?.cancel()
        var phaseChangeNotified = false
        
        // Set phaseStartTime to the current phase duration for accurate elapsed time calculation
        phaseStartTime = seconds
        
        // Calculate early notification time:
        // - Default: 2.5 seconds early (2500ms) to allow TTS to complete
        // - For short timers: Use (duration - 1 second) to avoid notifying before timer starts
        // - Minimum: 500ms to ensure we always notify before completion
        val notificationTime = minOf(2500L, (seconds - 1) * 1000L).coerceAtLeast(500L)
        
        countDownTimer = object : CountDownTimer(seconds * 1000L, 1000L) {
            @Suppress("UNUSED_VALUE")
            override fun onTick(millisUntilFinished: Long) {
                val remaining = (millisUntilFinished / 1000).toInt()
                val elapsedInPhase = phaseStartTime - remaining
                val currentElapsed = totalElapsedSeconds + elapsedInPhase
                val displayInterval = (currentIntervalIndex + 1).coerceAtMost(formula.totalIntervals)
                _state.value = _state.value.copy(
                    timeRemainingSeconds = remaining,
                    currentInterval = displayInterval,
                    elapsedSeconds = currentElapsed.coerceAtMost(formula.totalDurationSeconds)
                )
                
                // Early notification mechanism: Trigger phase change callbacks before the timer
                // actually finishes to allow TTS enough time to speak the message.
                // The phaseChangeNotified flag ensures we only notify once per phase.
                if (!phaseChangeNotified && millisUntilFinished <= notificationTime) {
                    if (isSlowPhase) {
                        // About to transition from slow phase: either to fast or to completion
                        if (isFinalSlowPhaseForFastStart()) {
                            onPhaseChange(IntervalPhase.Completed)
                        } else {
                            onPhaseChange(IntervalPhase.Fast)
                        }
                    } else {
                        // About to complete fast phase - either finish workout or start next interval
                        onPhaseChange(nextPhaseAfterFastCompletes())
                    }
                    // Mark as notified to prevent duplicate notifications in onFinish()
                    // The value is read in onFinish() via the closure, but compiler doesn't recognize it
                    phaseChangeNotified = true
                }
            }

            override fun onFinish() {
                // Failsafe: Ensure notification is sent if missed by onTick (e.g. due to lag)
                // Read phaseChangeNotified to check if we need to send notification
                val wasNotified = phaseChangeNotified
                if (!wasNotified) {
                    if (isSlowPhase) {
                        if (isFinalSlowPhaseForFastStart()) {
                            onPhaseChange(IntervalPhase.Completed)
                        } else {
                            onPhaseChange(IntervalPhase.Fast)
                        }
                    } else {
                        onPhaseChange(nextPhaseAfterFastCompletes())
                    }
                }

                // Update elapsed time: add the completed phase duration
                totalElapsedSeconds += phaseStartTime
                
                if (isSlowPhase) {
                    if (isFinalSlowPhaseForFastStart()) {
                        // Fast-start formulas finish on this trailing slow phase.
                        _state.value = _state.value.copy(
                            currentPhase = IntervalPhase.Completed,
                            isRunning = false,
                            timeRemainingSeconds = 0,
                            elapsedSeconds = formula.totalDurationSeconds
                        )
                        countDownTimer?.cancel()
                    } else {
                        // Transition from slow to fast phase within the same interval
                        isSlowPhase = false
                        phaseStartTime = formula.fastDurationSeconds
                        // Note: Phase change notification was already sent early (in onTick)
                        // Here we just update the state and start the next timer
                        startTimer(formula.fastDurationSeconds)
                        _state.value = _state.value.copy(
                            currentPhase = IntervalPhase.Fast,
                            timeRemainingSeconds = formula.fastDurationSeconds,
                            elapsedSeconds = totalElapsedSeconds
                        )
                    }
                } else {
                    // Fast phase completed
                    currentIntervalIndex++
                    onIntervalComplete()

                    val isLastFastComplete = currentIntervalIndex >= formula.totalIntervals &&
                        (formula.isCircuit || !formula.startsWithFast)
                    if (isLastFastComplete) {
                        // Circuit or slow-start: complete after last fast. Fast-start continues to trailing slow.
                        _state.value = _state.value.copy(
                            currentPhase = IntervalPhase.Completed,
                            isRunning = false,
                            timeRemainingSeconds = 0,
                            elapsedSeconds = formula.totalDurationSeconds
                        )
                        countDownTimer?.cancel()
                    } else if (formula.isCircuit && currentIntervalIndex % 2 == 0) {
                        // Circuit: just finished second fast of a round → start next round with Fast
                        isSlowPhase = false
                        phaseStartTime = formula.fastDurationSeconds
                        startTimer(formula.fastDurationSeconds)
                        val displayInterval = (currentIntervalIndex + 1).coerceAtMost(formula.totalIntervals)
                        _state.value = _state.value.copy(
                            currentPhase = IntervalPhase.Fast,
                            timeRemainingSeconds = formula.fastDurationSeconds,
                            currentInterval = displayInterval,
                            elapsedSeconds = totalElapsedSeconds
                        )
                    } else {
                        // Start next phase (slow): within-round or interval mode
                        isSlowPhase = true
                        phaseStartTime = formula.slowDurationSeconds
                        startTimer(formula.slowDurationSeconds)
                        val displayInterval = (currentIntervalIndex + 1).coerceAtMost(formula.totalIntervals)
                        _state.value = _state.value.copy(
                            currentPhase = IntervalPhase.Slow,
                            timeRemainingSeconds = formula.slowDurationSeconds,
                            currentInterval = displayInterval,
                            elapsedSeconds = totalElapsedSeconds
                        )
                    }
                }
            }
        }.start()
    }

    fun pause() {
        countDownTimer?.cancel()
        _state.value = _state.value.copy(isRunning = false)
    }

    fun reset() {
        countDownTimer?.cancel()
        currentIntervalIndex = 0
        isSlowPhase = !formula.startsWithFast
        totalElapsedSeconds = 0
        val startPhase = if (formula.startsWithFast) IntervalPhase.Fast else IntervalPhase.Slow
        val startTime = if (formula.startsWithFast) formula.fastDurationSeconds else formula.slowDurationSeconds
        phaseStartTime = startTime
        _state.value = TimerState(
            currentPhase = startPhase,
            timeRemainingSeconds = startTime,
            currentInterval = 0,
            totalIntervals = formula.totalIntervals,
            isRunning = false,
            elapsedSeconds = 0
        )
        onPhaseChange(startPhase)
    }

    fun restoreState(
        timeRemainingSeconds: Int,
        currentInterval: Int,
        currentPhase: IntervalPhase,
        isRunning: Boolean,
        savedElapsedSeconds: Int? = null
    ) {
        countDownTimer?.cancel()
        currentIntervalIndex = currentInterval - 1
        isSlowPhase = currentPhase is IntervalPhase.Slow
        phaseStartTime = if (isSlowPhase) formula.slowDurationSeconds else formula.fastDurationSeconds

        val computedElapsed = if (formula.isCircuit) {
            circuitElapsedSeconds(currentInterval, isSlowPhase, timeRemainingSeconds)
        } else {
            intervalModeElapsedSeconds(currentInterval, currentPhase, timeRemainingSeconds)
        }
        totalElapsedSeconds = (savedElapsedSeconds ?: computedElapsed).coerceIn(0, formula.totalDurationSeconds)

        _state.value = TimerState(
            currentPhase = currentPhase,
            timeRemainingSeconds = timeRemainingSeconds,
            currentInterval = currentInterval,
            totalIntervals = formula.totalIntervals,
            isRunning = false,
            elapsedSeconds = totalElapsedSeconds.coerceAtMost(formula.totalDurationSeconds)
        )

        onPhaseChange(currentPhase)

        // Resume timer if it was running
        if (isRunning && timeRemainingSeconds > 0) {
            resume(timeRemainingSeconds)
        }
    }

    /**
     * Elapsed seconds for circuit mode from (currentInterval, isSlowPhase, timeRemainingSeconds).
     * Each round has three phases (e.g. fast-slow-fast); currentInterval is 1-based, advances after each fast.
     */
    @Suppress("CyclomaticComplexMethod")
    private fun circuitElapsedSeconds(currentInterval: Int, isSlowPhase: Boolean, timeRemainingSeconds: Int): Int {
        val roundDuration = if (formula.startsWithFast) {
            2 * formula.fastDurationSeconds + formula.slowDurationSeconds
        } else {
            2 * formula.slowDurationSeconds + formula.fastDurationSeconds
        }
        val completedRounds = if (formula.startsWithFast) {
            (currentInterval - 1) / 2
        } else {
            if (currentInterval >= 2 && !isSlowPhase) (currentInterval / 2) - 1 else (currentInterval - 1) / 2
        }
        val elapsedInRound = if (currentInterval <= 0) 0
        else elapsedInCircuitRound(currentInterval, isSlowPhase, timeRemainingSeconds)
        return completedRounds * roundDuration + elapsedInRound.coerceIn(0, roundDuration)
    }

    /**
     * Elapsed seconds for interval mode from restored state.
     * For slow-start formulas, currentInterval is the active/display interval.
     */
    private fun intervalModeElapsedSeconds(
        currentInterval: Int,
        currentPhase: IntervalPhase,
        timeRemainingSeconds: Int
    ): Int {
        val slow = formula.slowDurationSeconds
        val fast = formula.fastDurationSeconds
        val cycleDuration = slow + fast

        val elapsed = when {
            currentPhase is IntervalPhase.Completed -> formula.totalDurationSeconds
            currentInterval <= 0 -> 0
            !formula.startsWithFast -> {
                when (currentPhase) {
                    is IntervalPhase.Slow ->
                        ((currentInterval - 1) * cycleDuration) + (slow - timeRemainingSeconds)
                    is IntervalPhase.Fast ->
                        ((currentInterval - 1) * cycleDuration) + slow + (fast - timeRemainingSeconds)
                    is IntervalPhase.Completed -> formula.totalDurationSeconds
                }
            }
            else -> {
                // Fast-start interval mode sequence: Fast(1), Slow(2), Fast(2), Slow(3), ... Fast(N), Slow(N trailing).
                when (currentPhase) {
                    is IntervalPhase.Fast -> ((currentInterval - 1) * cycleDuration) + (fast - timeRemainingSeconds)
                    is IntervalPhase.Slow -> {
                        if (currentInterval >= formula.totalIntervals) {
                            // Final trailing slow occurs after an additional fast phase.
                            ((currentInterval - 1).coerceAtLeast(0) * cycleDuration) +
                                fast + (slow - timeRemainingSeconds)
                        } else {
                            ((currentInterval - 2).coerceAtLeast(0) * cycleDuration) +
                                fast + (slow - timeRemainingSeconds)
                        }
                    }
                    is IntervalPhase.Completed -> formula.totalDurationSeconds
                }
            }
        }

        return elapsed.coerceIn(0, formula.totalDurationSeconds)
    }

    private fun elapsedInCircuitRound(currentInterval: Int, isSlowPhase: Boolean, timeRemainingSeconds: Int): Int {
        val fast = formula.fastDurationSeconds
        val slow = formula.slowDurationSeconds
        return if (formula.startsWithFast) {
            when {
                currentInterval % 2 == 1 && !isSlowPhase -> fast - timeRemainingSeconds
                isSlowPhase -> fast + (slow - timeRemainingSeconds)
                else -> fast + slow + (fast - timeRemainingSeconds)
            }
        } else {
            when {
                currentInterval % 2 == 1 && isSlowPhase -> slow - timeRemainingSeconds
                currentInterval % 2 == 1 && !isSlowPhase -> slow + (fast - timeRemainingSeconds)
                currentInterval % 2 == 0 && isSlowPhase ->
                    slow + fast + (slow - timeRemainingSeconds)
                else -> if (currentInterval == 2) fast - timeRemainingSeconds
                else slow + fast + (slow - timeRemainingSeconds)
            }
        }
    }

    fun dispose() {
        countDownTimer?.cancel()
    }
}

