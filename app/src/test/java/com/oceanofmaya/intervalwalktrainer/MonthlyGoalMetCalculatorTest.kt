package com.oceanofmaya.intervalwalktrainer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Calendar

class MonthlyGoalMetCalculatorTest {

    @Test
    fun `weekStartsInMonth lists every week starting in the month, including a boundary-crossing final week`() {
        val ranges = MonthlyGoalMetCalculator.weekStartsInMonth(2026, Calendar.MAY, Calendar.SUNDAY)

        assertEquals(5, ranges.size)
        assertEquals("2026-05-03", ranges.first().startDate)
        assertEquals("2026-05-09", ranges.first().endDate)
        // Final week starts in May but ends in June.
        assertEquals("2026-05-31", ranges.last().startDate)
        assertEquals("2026-06-06", ranges.last().endDate)
    }

    @Test
    fun `summarize reports inactive when goal disabled`() {
        val settings = WeeklyGoalSettings(enabled = false, targetWorkouts = 3, targetMinutes = 90)

        val summary = MonthlyGoalMetCalculator.summarize(settings, weeks(), NOW_2027)

        assertFalse(summary.active)
        assertEquals(0, summary.weeksMet)
        assertEquals(0, summary.totalWeeks)
    }

    @Test
    fun `summarize reports inactive when no targets are set`() {
        val settings = WeeklyGoalSettings(enabled = true, targetWorkouts = 0, targetMinutes = 0)

        val summary = MonthlyGoalMetCalculator.summarize(settings, weeks(window(workouts = 5, minutes = 200)), NOW_2027)

        assertFalse(summary.active)
    }

    @Test
    fun `summarize counts weeks meeting the workout target only`() {
        val settings = WeeklyGoalSettings(enabled = true, targetWorkouts = 3, targetMinutes = 0)

        val summary = MonthlyGoalMetCalculator.summarize(
            settings,
            weeks(window(workouts = 3, minutes = 10), window(workouts = 2, minutes = 999)),
            NOW_2027
        )

        assertTrue(summary.active)
        assertEquals(1, summary.weeksMet)
        assertEquals(2, summary.totalWeeks)
    }

    @Test
    fun `summarize counts weeks meeting the minute target only`() {
        val settings = WeeklyGoalSettings(enabled = true, targetWorkouts = 0, targetMinutes = 90)

        val summary = MonthlyGoalMetCalculator.summarize(
            settings,
            weeks(window(workouts = 0, minutes = 90), window(workouts = 9, minutes = 89)),
            NOW_2027
        )

        assertEquals(1, summary.weeksMet)
        assertEquals(2, summary.totalWeeks)
    }

    @Test
    fun `summarize requires both enabled targets to count as met`() {
        val settings = WeeklyGoalSettings(enabled = true, targetWorkouts = 3, targetMinutes = 90)

        val summary = MonthlyGoalMetCalculator.summarize(
            settings,
            weeks(
                window(workouts = 3, minutes = 90),
                window(workouts = 3, minutes = 80),
                window(workouts = 2, minutes = 100)
            ),
            NOW_2027
        )

        assertEquals(1, summary.weeksMet)
        assertEquals(3, summary.totalWeeks)
    }

    @Test
    fun `summarize excludes the in-progress current week`() {
        val settings = WeeklyGoalSettings(enabled = true, targetWorkouts = 1, targetMinutes = 0)
        val completed = WeekWindow(
            range = WeeklyDateRange("2026-05-03", "2026-05-09"),
            completedWorkouts = 1,
            completedMinutes = 0
        )
        val inProgress = WeekWindow(
            range = WeeklyDateRange("2026-05-17", "2026-05-23"),
            completedWorkouts = 1,
            completedMinutes = 0
        )

        val summary = MonthlyGoalMetCalculator.summarize(
            settings,
            listOf(completed, inProgress),
            millisFor(2026, Calendar.MAY, 20)
        )

        assertEquals(1, summary.weeksMet)
        assertEquals(1, summary.totalWeeks)
    }

    private fun weeks(vararg windows: WeekWindow): List<WeekWindow> = windows.toList()

    private fun window(workouts: Int, minutes: Int): WeekWindow {
        return WeekWindow(
            range = WeeklyDateRange("2026-05-03", "2026-05-09"),
            completedWorkouts = workouts,
            completedMinutes = minutes
        )
    }

    private fun millisFor(year: Int, month: Int, day: Int): Long {
        return Calendar.getInstance().apply {
            set(year, month, day, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private companion object {
        val NOW_2027: Long = Calendar.getInstance().apply {
            set(2027, Calendar.JANUARY, 1, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
