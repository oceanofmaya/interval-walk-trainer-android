package com.oceanofmaya.intervalwalktrainer

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Totals for a single Sunday-to-Saturday week used when evaluating monthly weekly-goal trends.
 */
data class WeekWindow(
    val range: WeeklyDateRange,
    val completedWorkouts: Int,
    val completedMinutes: Int
)

/**
 * Summary of how often the weekly goal was met during a single month.
 *
 * @param active whether a weekly goal with at least one target is configured.
 * @param weeksMet number of fully-completed weeks in the month whose targets were met.
 * @param totalWeeks number of fully-completed weeks in the month that were evaluated.
 */
data class MonthlyGoalMetSummary(
    val active: Boolean,
    val weeksMet: Int,
    val totalWeeks: Int
)

data class MonthlyGoalMetComparison(
    val current: MonthlyGoalMetSummary,
    val previous: MonthlyGoalMetSummary
)

/**
 * Calculates how often the weekly goal was met within a calendar month.
 *
 * Behaviour decisions (covered by unit tests):
 * - A week belongs to the month that contains its start day (the week's [WeeklyGoalSettings.weekStartDay]).
 *   This means a partial week at the start of a month whose Sunday lives in the previous month counts
 *   toward the previous month, and the final week of a month is counted here even when it spills into
 *   the next month.
 * - Only fully-completed weeks are evaluated. The in-progress current week (and any future week) is
 *   excluded so an unfinished week never counts as a miss.
 * - A week is "met" using the same rule as [WeeklyGoalProgress.isGoalMet]: every enabled target
 *   (workouts and/or minutes) must be reached.
 */
object MonthlyGoalMetCalculator {
    private const val DAYS_PER_WEEK = 7

    fun weekStartsInMonth(year: Int, month: Int, weekStartDay: Int): List<WeeklyDateRange> {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val calendar = Calendar.getInstance().apply {
            firstDayOfWeek = weekStartDay
            set(year, month, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // Advance to the first week-start on or after the first day of the month.
        while (calendar.get(Calendar.DAY_OF_WEEK) != weekStartDay) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }

        val ranges = mutableListOf<WeeklyDateRange>()
        while (calendar.get(Calendar.MONTH) == month && calendar.get(Calendar.YEAR) == year) {
            val start = formatter.format(calendar.time)
            val endCalendar = (calendar.clone() as Calendar).apply {
                add(Calendar.DAY_OF_MONTH, DAYS_PER_WEEK - 1)
            }
            ranges += WeeklyDateRange(startDate = start, endDate = formatter.format(endCalendar.time))
            calendar.add(Calendar.DAY_OF_MONTH, DAYS_PER_WEEK)
        }
        return ranges
    }

    fun isWeekMet(settings: WeeklyGoalSettings, window: WeekWindow): Boolean {
        val workoutsMet = !settings.tracksWorkouts || window.completedWorkouts >= settings.targetWorkouts
        val minutesMet = !settings.tracksMinutes || window.completedMinutes >= settings.targetMinutes
        return workoutsMet && minutesMet
    }

    fun summarize(
        settings: WeeklyGoalSettings,
        weeks: List<WeekWindow>,
        nowMillis: Long = System.currentTimeMillis()
    ): MonthlyGoalMetSummary {
        if (!settings.enabled || !settings.hasAnyTarget) {
            return MonthlyGoalMetSummary(active = false, weeksMet = 0, totalWeeks = 0)
        }
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(nowMillis))
        var weeksMet = 0
        var totalWeeks = 0
        weeks.forEach { window ->
            // Skip the in-progress current week and any future week.
            if (window.range.endDate >= today) {
                return@forEach
            }
            totalWeeks++
            if (isWeekMet(settings, window)) {
                weeksMet++
            }
        }
        return MonthlyGoalMetSummary(active = true, weeksMet = weeksMet, totalWeeks = totalWeeks)
    }
}
