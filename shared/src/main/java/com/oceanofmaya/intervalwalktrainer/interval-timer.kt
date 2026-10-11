package com.oceanofmaya.intervalwalktrainer

import android.os.CountDownTimer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class IntervalPhase {
    data object Slow : IntervalPhase()
    data object Fast : IntervalPhase()
    data object Completed : IntervalPhase()
}

data class TimerState(
    val currentPhase: IntervalPhase = IntervalPhase.Slow,
    val timeRemainingSeconds: Int = 0,
    val currentInterval: Int = 0,
    val totalIntervals: Int = 0,
    val isRunning: Boolean = false,
    val elapsedSeconds: Int = 0
)

/**
 * Shared phone/watch interval timer. [onPhaseChange] may be called early for announcements;
 * [onWorkoutComplete] is called only after the timer reaches its actual completed state.
 */
class IntervalTimer(
    private val formula: IntervalFormula,
    private val onPhaseChange: (IntervalPhase) -> Unit,
    private val onIntervalComplete: () -> Unit,
    private val onWorkoutComplete: () -> Unit = {}
) {
    private var countDownTimer: CountDownTimer? = null
    private val initialPhase = if (formula.startsWithFast) IntervalPhase.Fast else IntervalPhase.Slow
    private val initialTime = if (formula.startsWithFast) formula.fastDurationSeconds else formula.slowDurationSeconds
    private val _state = MutableStateFlow(
        TimerState(
            currentPhase = initialPhase,
            totalIntervals = formula.totalIntervals,
            timeRemainingSeconds = initialTime
        )
    )
    val state: StateFlow<TimerState> = _state.asStateFlow()

    private var currentIntervalIndex = 0
    private var isSlowPhase = !formula.startsWithFast
    private var totalElapsedSeconds = 0
    private var phaseStartTime = 0

    private fun isFinalSlowPhaseForFastStart(): Boolean {
        if (formula.isCircuit) return false
        return formula.startsWithFast && isSlowPhase && currentIntervalIndex >= formula.totalIntervals
    }

    private fun nextPhaseAfterFastCompletes(): IntervalPhase {
        val nextIndex = currentIntervalIndex + 1
        if (nextIndex >= formula.totalIntervals) {
            return if (formula.isCircuit || !formula.startsWithFast) {
                IntervalPhase.Completed
            } else {
                IntervalPhase.Slow
            }
        }
        return if (formula.isCircuit && nextIndex % 2 == 0) {
            IntervalPhase.Fast
        } else {
            IntervalPhase.Slow
        }
    }

    fun start() {
        if (_state.value.isRunning) return

        val currentTime = _state.value.timeRemainingSeconds
        if (currentTime > 0) {
            if (_state.value.elapsedSeconds == 0) {
                onPhaseChange(_state.value.currentPhase)
            }
            resume(currentTime)
        } else {
            countDownTimer?.cancel()
            currentIntervalIndex = 0
            isSlowPhase = !formula.startsWithFast
            val startPhase = if (formula.startsWithFast) IntervalPhase.Fast else IntervalPhase.Slow
            val startTime = if (formula.startsWithFast) formula.fastDurationSeconds else formula.slowDurationSeconds
            totalElapsedSeconds = 0
            phaseStartTime = startTime
            _state.value = TimerState(
                currentPhase = startPhase,
                timeRemainingSeconds = startTime,
                totalIntervals = formula.totalIntervals
            )
            onPhaseChange(startPhase)
            resume(startTime)
        }
    }

    private fun resume(seconds: Int) {
        _state.value = _state.value.copy(isRunning = true)
        startTimer(seconds)
    }

    private fun startTimer(seconds: Int) {
        countDownTimer?.cancel()
        var phaseChangeNotified = false
        phaseStartTime = seconds
        val notificationTime = minOf(2500L, (seconds - 1) * 1000L).coerceAtLeast(500L)

        countDownTimer = object : CountDownTimer(seconds * 1000L, 1000L) {
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

                if (!phaseChangeNotified && millisUntilFinished <= notificationTime) {
                    notifyUpcomingPhase()
                    phaseChangeNotified = true
                }
            }

            override fun onFinish() {
                if (!phaseChangeNotified) {
                    notifyUpcomingPhase()
                }
                totalElapsedSeconds += phaseStartTime

                if (isSlowPhase) {
                    if (isFinalSlowPhaseForFastStart()) {
                        completeWorkout()
                    } else {
                        isSlowPhase = false
                        phaseStartTime = formula.fastDurationSeconds
                        startTimer(formula.fastDurationSeconds)
                        _state.value = _state.value.copy(
                            currentPhase = IntervalPhase.Fast,
                            timeRemainingSeconds = formula.fastDurationSeconds,
                            elapsedSeconds = totalElapsedSeconds
                        )
                    }
                } else {
                    currentIntervalIndex++
                    onIntervalComplete()
                    val isLastFastComplete = currentIntervalIndex >= formula.totalIntervals &&
                        (formula.isCircuit || !formula.startsWithFast)
                    when {
                        isLastFastComplete -> completeWorkout()
                        formula.isCircuit && currentIntervalIndex % 2 == 0 -> {
                            isSlowPhase = false
                            phaseStartTime = formula.fastDurationSeconds
                            startTimer(formula.fastDurationSeconds)
                            _state.value = _state.value.copy(
                                currentPhase = IntervalPhase.Fast,
                                timeRemainingSeconds = formula.fastDurationSeconds,
                                currentInterval = (currentIntervalIndex + 1)
                                    .coerceAtMost(formula.totalIntervals),
                                elapsedSeconds = totalElapsedSeconds
                            )
                        }
                        else -> {
                            isSlowPhase = true
                            phaseStartTime = formula.slowDurationSeconds
                            startTimer(formula.slowDurationSeconds)
                            _state.value = _state.value.copy(
                                currentPhase = IntervalPhase.Slow,
                                timeRemainingSeconds = formula.slowDurationSeconds,
                                currentInterval = (currentIntervalIndex + 1)
                                    .coerceAtMost(formula.totalIntervals),
                                elapsedSeconds = totalElapsedSeconds
                            )
                        }
                    }
                }
            }

            private fun notifyUpcomingPhase() {
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
        }.start()
    }

    private fun completeWorkout() {
        _state.value = _state.value.copy(
            currentPhase = IntervalPhase.Completed,
            isRunning = false,
            timeRemainingSeconds = 0,
            elapsedSeconds = formula.totalDurationSeconds
        )
        countDownTimer?.cancel()
        onWorkoutComplete()
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
            totalIntervals = formula.totalIntervals
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
        currentIntervalIndex = (currentInterval - 1).coerceAtLeast(0)
        isSlowPhase = currentPhase is IntervalPhase.Slow
        phaseStartTime = if (isSlowPhase) formula.slowDurationSeconds else formula.fastDurationSeconds

        val computedElapsed = if (formula.isCircuit) {
            circuitElapsedSeconds(currentInterval, isSlowPhase, timeRemainingSeconds)
        } else {
            intervalModeElapsedSeconds(currentInterval, currentPhase, timeRemainingSeconds)
        }
        totalElapsedSeconds = (savedElapsedSeconds ?: computedElapsed)
            .coerceIn(0, formula.totalDurationSeconds)

        _state.value = TimerState(
            currentPhase = currentPhase,
            timeRemainingSeconds = timeRemainingSeconds,
            currentInterval = currentInterval,
            totalIntervals = formula.totalIntervals,
            elapsedSeconds = totalElapsedSeconds
        )
        onPhaseChange(currentPhase)
        if (isRunning && timeRemainingSeconds > 0) {
            resume(timeRemainingSeconds)
        }
    }

    private fun circuitElapsedSeconds(
        currentInterval: Int,
        isSlowPhase: Boolean,
        timeRemainingSeconds: Int
    ): Int {
        val roundDuration = if (formula.startsWithFast) {
            2 * formula.fastDurationSeconds + formula.slowDurationSeconds
        } else {
            2 * formula.slowDurationSeconds + formula.fastDurationSeconds
        }
        val completedRounds = if (formula.startsWithFast) {
            (currentInterval - 1) / 2
        } else {
            if (currentInterval >= 2 && !isSlowPhase) {
                (currentInterval / 2) - 1
            } else {
                (currentInterval - 1) / 2
            }
        }
        val elapsedInRound = if (currentInterval <= 0) {
            0
        } else {
            elapsedInCircuitRound(currentInterval, isSlowPhase, timeRemainingSeconds)
        }
        return completedRounds * roundDuration + elapsedInRound.coerceIn(0, roundDuration)
    }

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
            !formula.startsWithFast -> when (currentPhase) {
                is IntervalPhase.Slow ->
                    ((currentInterval - 1) * cycleDuration) + (slow - timeRemainingSeconds)
                is IntervalPhase.Fast ->
                    ((currentInterval - 1) * cycleDuration) + slow + (fast - timeRemainingSeconds)
                is IntervalPhase.Completed -> formula.totalDurationSeconds
            }
            else -> when (currentPhase) {
                is IntervalPhase.Fast ->
                    ((currentInterval - 1) * cycleDuration) + (fast - timeRemainingSeconds)
                is IntervalPhase.Slow -> {
                    if (currentInterval >= formula.totalIntervals) {
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
        return elapsed.coerceIn(0, formula.totalDurationSeconds)
    }

    private fun elapsedInCircuitRound(
        currentInterval: Int,
        isSlowPhase: Boolean,
        timeRemainingSeconds: Int
    ): Int {
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
                currentInterval == 2 -> fast - timeRemainingSeconds
                else -> slow + fast + (slow - timeRemainingSeconds)
            }
        }
    }

    fun dispose() {
        countDownTimer?.cancel()
    }
}
