package com.oceanofmaya.intervalwalktrainer

enum class WorkoutNotificationPhase {
    SLOW,
    FAST
}

enum class WorkoutNotificationAction {
    PAUSE,
    RESUME
}

data class WorkoutNotificationState(
    val phase: WorkoutNotificationPhase,
    val remainingSeconds: Int,
    val currentPosition: Int,
    val totalPositions: Int,
    val isCircuit: Boolean,
    val isRunning: Boolean
) {
    val action: WorkoutNotificationAction
        get() = if (isRunning) WorkoutNotificationAction.PAUSE else WorkoutNotificationAction.RESUME

    fun shouldReplace(previous: WorkoutNotificationState?): Boolean {
        if (previous == null) return true
        return phase != previous.phase ||
            currentPosition != previous.currentPosition ||
            totalPositions != previous.totalPositions ||
            isCircuit != previous.isCircuit ||
            isRunning != previous.isRunning ||
            (!isRunning && remainingSeconds != previous.remainingSeconds)
    }

    companion object {
        fun fromTimer(state: TimerState, isCircuit: Boolean): WorkoutNotificationState? {
            val phase = when (state.currentPhase) {
                IntervalPhase.Slow -> WorkoutNotificationPhase.SLOW
                IntervalPhase.Fast -> WorkoutNotificationPhase.FAST
                IntervalPhase.Completed -> return null
            }
            val totalPositions = if (isCircuit) {
                (state.totalIntervals / 2).coerceAtLeast(1)
            } else {
                state.totalIntervals.coerceAtLeast(1)
            }
            val currentPosition = if (isCircuit) {
                if (state.currentInterval <= 0) {
                    1
                } else {
                    (((state.currentInterval - 1) / 2) + 1).coerceIn(1, totalPositions)
                }
            } else {
                state.currentInterval.coerceAtLeast(1).coerceAtMost(totalPositions)
            }
            return WorkoutNotificationState(
                phase = phase,
                remainingSeconds = state.timeRemainingSeconds.coerceAtLeast(0),
                currentPosition = currentPosition,
                totalPositions = totalPositions,
                isCircuit = isCircuit,
                isRunning = state.isRunning
            )
        }
    }
}
