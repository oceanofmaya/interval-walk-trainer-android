package com.oceanofmaya.intervalwalktrainer

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.mock
import java.util.Calendar
import java.util.Locale

@ExtendWith(MockitoExtension::class)
class WeeklyGoalUiFormatterTest {

    private val formatter = WeeklyGoalUiFormatter(mock())

    @Test
    fun `isLastGoalDay is true on Saturday when week starts on Sunday`() {
        val progress = weeklyProgress(endDate = "2026-05-23")

        assertTrue(formatter.isLastGoalDay(progress, millisFor(2026, Calendar.MAY, 23)))
    }

    @Test
    fun `isLastGoalDay is false on Sunday when week starts on Sunday`() {
        val progress = weeklyProgress(endDate = "2026-05-23")

        assertFalse(formatter.isLastGoalDay(progress, millisFor(2026, Calendar.MAY, 24)))
    }

    @Test
    fun `isLastGoalDay is false when goal is already met`() {
        val progress = weeklyProgress(
            endDate = "2026-05-23",
            completedWorkouts = 3,
            completedMinutes = 90
        )

        assertFalse(formatter.isLastGoalDay(progress, millisFor(2026, Calendar.MAY, 23)))
    }

    @Test
    fun `isLastGoalDay is true on Sunday when week starts on Monday`() {
        val progress = weeklyProgress(endDate = "2026-05-24", weekStartDay = Calendar.MONDAY)

        assertTrue(formatter.isLastGoalDay(progress, millisFor(2026, Calendar.MAY, 24)))
    }

    private fun weeklyProgress(
        endDate: String,
        weekStartDay: Int = Calendar.SUNDAY,
        completedWorkouts: Int = 0,
        completedMinutes: Int = 0
    ): WeeklyGoalProgress {
        val startCalendar = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.MAY)
            set(Calendar.DAY_OF_MONTH, endDate.substringAfterLast("-").toInt())
            add(Calendar.DAY_OF_MONTH, -6)
        }
        val startDate = String.format(
            Locale.US,
            "%04d-%02d-%02d",
            startCalendar.get(Calendar.YEAR),
            startCalendar.get(Calendar.MONTH) + 1,
            startCalendar.get(Calendar.DAY_OF_MONTH)
        )
        return WeeklyGoalProgress(
            settings = WeeklyGoalSettings(
                enabled = true,
                targetWorkouts = 3,
                targetMinutes = 90,
                weekStartDay = weekStartDay
            ),
            dateRange = WeeklyDateRange(startDate = startDate, endDate = endDate),
            completedWorkouts = completedWorkouts,
            completedMinutes = completedMinutes
        )
    }

    private fun millisFor(
        year: Int,
        month: Int,
        day: Int,
        hour: Int = 12,
        minute: Int = 0
    ): Long {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
