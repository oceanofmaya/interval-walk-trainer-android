package com.oceanofmaya.intervalwalktrainer.home

import android.content.SharedPreferences
import com.oceanofmaya.intervalwalktrainer.WorkoutRepository

class HomeInsightRegistry(
    sharedPreferences: SharedPreferences,
    workoutRepository: WorkoutRepository,
    accentColorProvider: () -> Int,
    onEditWeeklyGoal: () -> Unit
) {
    private val cards: List<HomeInsightCard> = listOf(
        WeeklyGoalHomeInsight(
            sharedPreferences = sharedPreferences,
            workoutRepository = workoutRepository,
            accentColorProvider = accentColorProvider,
            onEditWeeklyGoal = onEditWeeklyGoal
        )
    )

    fun all(): List<HomeInsightCard> = cards
}
