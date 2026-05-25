package com.oceanofmaya.intervalwalktrainer.home

import com.oceanofmaya.intervalwalktrainer.WorkoutRecord
import com.oceanofmaya.intervalwalktrainer.WorkoutRepository
import com.oceanofmaya.intervalwalktrainer.WorkoutSession
import com.oceanofmaya.intervalwalktrainer.WorkoutStatistics
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@ExtendWith(MockitoExtension::class)
class CurrentStreakHomeInsightTest {
    @Mock
    private lateinit var workoutRepository: WorkoutRepository

    @Test
    fun `isEligible always returns true when save workouts disabled`() = runTest {
        val insight = createInsight(saveWorkoutsEnabled = false)

        assertTrue(insight.isEligible())
    }

    @Test
    fun `isEligible always returns true when statistics available`() = runTest {
        whenever(workoutRepository.getStatistics()).thenReturn(
            WorkoutStatistics(
                totalWorkouts = 5,
                totalMinutes = 100,
                currentStreak = 3,
                longestStreak = 5,
                averageWorkoutsPerWeek = 2.0,
                earliestDate = "2024-01-01",
                latestDate = "2024-01-05"
            )
        )
        val insight = createInsight(saveWorkoutsEnabled = true)

        assertTrue(insight.isEligible())
    }

    private fun createInsight(saveWorkoutsEnabled: Boolean): CurrentStreakHomeInsight {
        return CurrentStreakHomeInsight(
            workoutRepository = workoutRepository,
            saveWorkoutsEnabledProvider = { saveWorkoutsEnabled },
            accentColorProvider = { 0xFF0000FF.toInt() },
            onOpenWorkoutHistory = {}
        )
    }
}

@ExtendWith(MockitoExtension::class)
class TodayHomeInsightTest {
    @Mock
    private lateinit var workoutRepository: WorkoutRepository

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    @Test
    fun `isEligible returns true when today has no workouts yet`() = runTest {
        val today = dateFormat.format(Date())
        whenever(workoutRepository.getRecordByDate(today)).thenReturn(null)
        val insight = TodayHomeInsight(
            workoutRepository = workoutRepository,
            saveWorkoutsEnabledProvider = { true },
            accentColorProvider = { 0xFF0000FF.toInt() },
            onOpenWorkoutHistory = {},
            onOpenWorkoutDate = {}
        )

        assertTrue(insight.isEligible())
    }

    @Test
    fun `isEligible returns true when today has workouts`() = runTest {
        val today = dateFormat.format(Date())
        whenever(workoutRepository.getRecordByDate(today)).thenReturn(
            WorkoutRecord(date = today, completedWorkouts = 1, totalMinutes = 28)
        )
        val insight = TodayHomeInsight(
            workoutRepository = workoutRepository,
            saveWorkoutsEnabledProvider = { true },
            accentColorProvider = { 0xFF0000FF.toInt() },
            onOpenWorkoutHistory = {},
            onOpenWorkoutDate = {}
        )

        assertTrue(insight.isEligible())
    }
}

@ExtendWith(MockitoExtension::class)
class LastWorkoutHomeInsightTest {
    @Mock
    private lateinit var workoutRepository: WorkoutRepository

    @Test
    fun `isEligible returns true when latest session exists`() = runTest {
        whenever(workoutRepository.getLatestSession()).thenReturn(
            WorkoutSession(
                date = "2024-01-02",
                workoutType = "3-3 Japanese - 5 Rounds (30 min)",
                minutes = 30
            )
        )
        val insight = LastWorkoutHomeInsight(
            workoutRepository = workoutRepository,
            saveWorkoutsEnabledProvider = { true },
            accentColorProvider = { 0xFF0000FF.toInt() },
            onOpenWorkoutHistory = {},
            onOpenWorkoutDate = {}
        )

        assertTrue(insight.isEligible())
    }

    @Test
    fun `isEligible returns true when no sessions exist`() = runTest {
        whenever(workoutRepository.getLatestSession()).thenReturn(null)
        val insight = LastWorkoutHomeInsight(
            workoutRepository = workoutRepository,
            saveWorkoutsEnabledProvider = { true },
            accentColorProvider = { 0xFF0000FF.toInt() },
            onOpenWorkoutHistory = {},
            onOpenWorkoutDate = {}
        )

        assertTrue(insight.isEligible())
    }
}
