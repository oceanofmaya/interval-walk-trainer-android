package com.oceanofmaya.intervalwalktrainer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WeeklyGoalProgressTest {

    private val dateRange = WeeklyDateRange(startDate = "2026-05-19", endDate = "2026-05-25")

    @Test
    fun `displayed completed values cap at target when goal is exceeded`() {
        val progress = WeeklyGoalProgress(
            settings = WeeklyGoalSettings(
                enabled = true,
                targetWorkouts = 3,
                targetMinutes = 90
            ),
            dateRange = dateRange,
            completedWorkouts = 4,
            completedMinutes = 120
        )

        assertEquals(3, progress.displayedCompletedWorkouts)
        assertEquals(90, progress.displayedCompletedMinutes)
        assertTrue(progress.isGoalMet)
    }

    @Test
    fun `progress percent caps at one hundred when target is exceeded`() {
        val progress = WeeklyGoalProgress(
            settings = WeeklyGoalSettings(
                enabled = true,
                targetWorkouts = 3,
                targetMinutes = 90
            ),
            dateRange = dateRange,
            completedWorkouts = 5,
            completedMinutes = 150
        )

        assertEquals(100, progress.workoutPercent)
        assertEquals(100, progress.minutesPercent)
    }

    @Test
    fun `displayed completed values are zero when target type is disabled`() {
        val progress = WeeklyGoalProgress(
            settings = WeeklyGoalSettings(
                enabled = true,
                targetWorkouts = 0,
                targetMinutes = 60
            ),
            dateRange = dateRange,
            completedWorkouts = 2,
            completedMinutes = 75
        )

        assertEquals(0, progress.displayedCompletedWorkouts)
        assertEquals(60, progress.displayedCompletedMinutes)
    }
}
