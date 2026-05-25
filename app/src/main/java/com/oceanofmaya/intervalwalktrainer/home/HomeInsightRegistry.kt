package com.oceanofmaya.intervalwalktrainer.home

import android.content.SharedPreferences
import com.oceanofmaya.intervalwalktrainer.WorkoutRepository

class HomeInsightRegistry(
    sharedPreferences: SharedPreferences,
    workoutRepository: WorkoutRepository,
    accentColorProvider: () -> Int,
    saveWorkoutsEnabledProvider: () -> Boolean,
    onEditWeeklyGoal: () -> Unit,
    onOpenWorkoutHistory: () -> Unit,
    onOpenWorkoutDate: (String) -> Unit
) {
    private val cards: List<HomeInsightCard> = listOf(
        WeeklyGoalHomeInsight(
            sharedPreferences = sharedPreferences,
            workoutRepository = workoutRepository,
            accentColorProvider = accentColorProvider,
            onEditWeeklyGoal = onEditWeeklyGoal
        ),
        CurrentStreakHomeInsight(
            workoutRepository = workoutRepository,
            saveWorkoutsEnabledProvider = saveWorkoutsEnabledProvider,
            accentColorProvider = accentColorProvider,
            onOpenWorkoutHistory = onOpenWorkoutHistory
        ),
        TodayHomeInsight(
            workoutRepository = workoutRepository,
            saveWorkoutsEnabledProvider = saveWorkoutsEnabledProvider,
            accentColorProvider = accentColorProvider,
            onOpenWorkoutHistory = onOpenWorkoutHistory,
            onOpenWorkoutDate = onOpenWorkoutDate
        ),
        LastWorkoutHomeInsight(
            workoutRepository = workoutRepository,
            saveWorkoutsEnabledProvider = saveWorkoutsEnabledProvider,
            accentColorProvider = accentColorProvider,
            onOpenWorkoutHistory = onOpenWorkoutHistory,
            onOpenWorkoutDate = onOpenWorkoutDate
        )
    )

    fun all(): List<HomeInsightCard> = cards
}
