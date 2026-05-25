package com.oceanofmaya.intervalwalktrainer

import android.content.SharedPreferences
import androidx.core.content.edit
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

data class WeeklyGoalSettings(
    val enabled: Boolean = false,
    val targetWorkouts: Int = WeeklyGoalPreferences.DEFAULT_TARGET_WORKOUTS,
    val targetMinutes: Int = WeeklyGoalPreferences.DEFAULT_TARGET_MINUTES,
    val weekStartDay: Int = Calendar.SUNDAY
) {
    val tracksWorkouts: Boolean
        get() = targetWorkouts > 0

    val tracksMinutes: Boolean
        get() = targetMinutes > 0

    val hasAnyTarget: Boolean
        get() = tracksWorkouts || tracksMinutes
}

fun WeeklyGoalSettings.remindersAvailable(): Boolean = enabled && hasAnyTarget

fun canScheduleWeeklyReminders(
    goalSettings: WeeklyGoalSettings,
    reminderSettings: WeeklyReminderSettings,
    isGoalMet: Boolean
): Boolean {
    return reminderSettings.enabled &&
        goalSettings.remindersAvailable() &&
        !(reminderSettings.pauseWhenGoalMet && isGoalMet)
}

data class WeeklyReminderSettings(
    val enabled: Boolean = false,
    val hourOfDay: Int = WeeklyGoalPreferences.DEFAULT_REMINDER_HOUR,
    val minute: Int = WeeklyGoalPreferences.DEFAULT_REMINDER_MINUTE,
    val selectedDays: Set<Int> = WeeklyGoalPreferences.DEFAULT_REMINDER_DAYS,
    val pauseWhenGoalMet: Boolean = true
)

data class WeeklyDateRange(
    val startDate: String,
    val endDate: String
)

data class WeeklyGoalProgress(
    val settings: WeeklyGoalSettings,
    val dateRange: WeeklyDateRange,
    val completedWorkouts: Int,
    val completedMinutes: Int
) {
    val workoutPercent: Int
        get() = percent(completedWorkouts, settings.targetWorkouts)

    val minutesPercent: Int
        get() = percent(completedMinutes, settings.targetMinutes)

    val isGoalMet: Boolean
        get() {
            if (!settings.enabled) return false
            val workoutsMet = !settings.tracksWorkouts || completedWorkouts >= settings.targetWorkouts
            val minutesMet = !settings.tracksMinutes || completedMinutes >= settings.targetMinutes
            return workoutsMet && minutesMet
        }

    val displayedCompletedWorkouts: Int
        get() = if (settings.tracksWorkouts) {
            completedWorkouts.coerceAtMost(settings.targetWorkouts)
        } else {
            0
        }

    val displayedCompletedMinutes: Int
        get() = if (settings.tracksMinutes) {
            completedMinutes.coerceAtMost(settings.targetMinutes)
        } else {
            0
        }

    val hasAnyTarget: Boolean
        get() = settings.tracksWorkouts || settings.tracksMinutes

    private fun percent(current: Int, target: Int): Int {
        if (target <= 0) return 0
        return ((current.toFloat() / target.toFloat()) * PERCENT_MAX).roundToInt().coerceIn(0, PERCENT_MAX)
    }

    companion object {
        private const val PERCENT_MAX = 100
    }
}

object WeeklyGoalPreferences {
    const val PREFS_NAME = "interval_walk_trainer_prefs"
    const val KEY_WEEKLY_GOAL_ENABLED = "weekly_goal_enabled"
    const val KEY_WEEKLY_GOAL_TARGET_WORKOUTS = "weekly_goal_target_workouts"
    const val KEY_WEEKLY_GOAL_TARGET_MINUTES = "weekly_goal_target_minutes"
    const val KEY_WEEKLY_GOAL_WEEK_START_DAY = "weekly_goal_week_start_day"
    const val KEY_WEEKLY_REMINDER_ENABLED = "weekly_reminder_enabled"
    const val KEY_WEEKLY_REMINDER_HOUR = "weekly_reminder_hour"
    const val KEY_WEEKLY_REMINDER_MINUTE = "weekly_reminder_minute"
    const val KEY_WEEKLY_REMINDER_DAYS = "weekly_reminder_days"
    const val KEY_WEEKLY_REMINDER_PAUSE_WHEN_MET = "weekly_reminder_pause_when_met"

    const val DEFAULT_TARGET_WORKOUTS = 3
    const val DEFAULT_TARGET_MINUTES = 90
    const val DEFAULT_REMINDER_HOUR = 18
    const val DEFAULT_REMINDER_MINUTE = 0
    val DEFAULT_REMINDER_DAYS: Set<Int> = setOf(Calendar.MONDAY, Calendar.WEDNESDAY, Calendar.FRIDAY)

    fun loadGoalSettings(sharedPreferences: SharedPreferences): WeeklyGoalSettings {
        val weekStartDay = loadWeekStartDay(sharedPreferences)
        return WeeklyGoalSettings(
            enabled = sharedPreferences.getBoolean(KEY_WEEKLY_GOAL_ENABLED, false),
            targetWorkouts = sharedPreferences.getInt(KEY_WEEKLY_GOAL_TARGET_WORKOUTS, DEFAULT_TARGET_WORKOUTS)
                .coerceIn(0, MAX_TARGET_WORKOUTS),
            targetMinutes = sharedPreferences.getInt(KEY_WEEKLY_GOAL_TARGET_MINUTES, DEFAULT_TARGET_MINUTES)
                .coerceIn(0, MAX_TARGET_MINUTES),
            weekStartDay = weekStartDay
        )
    }

    fun saveGoalSettings(sharedPreferences: SharedPreferences, settings: WeeklyGoalSettings) {
        sharedPreferences.edit {
            putBoolean(KEY_WEEKLY_GOAL_ENABLED, settings.enabled)
            putInt(KEY_WEEKLY_GOAL_TARGET_WORKOUTS, settings.targetWorkouts.coerceIn(0, MAX_TARGET_WORKOUTS))
            putInt(KEY_WEEKLY_GOAL_TARGET_MINUTES, settings.targetMinutes.coerceIn(0, MAX_TARGET_MINUTES))
            putInt(KEY_WEEKLY_GOAL_WEEK_START_DAY, Calendar.SUNDAY)
        }
    }

    private fun loadWeekStartDay(sharedPreferences: SharedPreferences): Int {
        val storedWeekStartDay = sharedPreferences.getInt(KEY_WEEKLY_GOAL_WEEK_START_DAY, Calendar.SUNDAY)
        if (storedWeekStartDay != Calendar.SUNDAY) {
            sharedPreferences.edit {
                putInt(KEY_WEEKLY_GOAL_WEEK_START_DAY, Calendar.SUNDAY)
            }
        }
        return Calendar.SUNDAY
    }

    fun loadReminderSettings(sharedPreferences: SharedPreferences): WeeklyReminderSettings {
        return WeeklyReminderSettings(
            enabled = sharedPreferences.getBoolean(KEY_WEEKLY_REMINDER_ENABLED, false),
            hourOfDay = sharedPreferences.getInt(KEY_WEEKLY_REMINDER_HOUR, DEFAULT_REMINDER_HOUR)
                .coerceIn(0, HOURS_PER_DAY - 1),
            minute = sharedPreferences.getInt(KEY_WEEKLY_REMINDER_MINUTE, DEFAULT_REMINDER_MINUTE)
                .coerceIn(0, MINUTES_PER_HOUR - 1),
            selectedDays = parseReminderDays(
                sharedPreferences.getString(KEY_WEEKLY_REMINDER_DAYS, null)
            ),
            pauseWhenGoalMet = sharedPreferences.getBoolean(KEY_WEEKLY_REMINDER_PAUSE_WHEN_MET, true)
        )
    }

    fun saveReminderSettings(sharedPreferences: SharedPreferences, settings: WeeklyReminderSettings) {
        sharedPreferences.edit {
            putBoolean(KEY_WEEKLY_REMINDER_ENABLED, settings.enabled)
            putInt(KEY_WEEKLY_REMINDER_HOUR, settings.hourOfDay.coerceIn(0, HOURS_PER_DAY - 1))
            putInt(KEY_WEEKLY_REMINDER_MINUTE, settings.minute.coerceIn(0, MINUTES_PER_HOUR - 1))
            putString(KEY_WEEKLY_REMINDER_DAYS, settings.selectedDays.sorted().joinToString(","))
            putBoolean(KEY_WEEKLY_REMINDER_PAUSE_WHEN_MET, settings.pauseWhenGoalMet)
        }
    }

    private fun parseReminderDays(raw: String?): Set<Int> {
        if (raw.isNullOrBlank()) return DEFAULT_REMINDER_DAYS
        val days = raw.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .filter { it in Calendar.SUNDAY..Calendar.SATURDAY }
            .toSet()
        return days.ifEmpty { DEFAULT_REMINDER_DAYS }
    }

    private const val HOURS_PER_DAY = 24
    private const val MINUTES_PER_HOUR = 60
    private const val MAX_TARGET_WORKOUTS = 14
    private const val MAX_TARGET_MINUTES = 1000
}

object WeeklyGoalCalculator {
    private const val DAYS_PER_WEEK = 7
    private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L

    fun currentWeekRange(
        nowMillis: Long = System.currentTimeMillis(),
        weekStartDay: Int = Calendar.SUNDAY
    ): WeeklyDateRange {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            firstDayOfWeek = weekStartDay
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        while (calendar.get(Calendar.DAY_OF_WEEK) != weekStartDay) {
            calendar.add(Calendar.DAY_OF_MONTH, -1)
        }
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val start = formatter.format(calendar.time)
        calendar.add(Calendar.DAY_OF_MONTH, DAYS_PER_WEEK - 1)
        val end = formatter.format(calendar.time)
        return WeeklyDateRange(startDate = start, endDate = end)
    }

    fun nextReminderTimeMillis(
        settings: WeeklyReminderSettings,
        nowMillis: Long = System.currentTimeMillis()
    ): Long? {
        if (!settings.enabled || settings.selectedDays.isEmpty()) {
            return null
        }
        val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val candidate = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        var nextReminder: Long? = null
        for (dayOffset in 0 until DAYS_PER_WEEK) {
            candidate.timeInMillis = nowMillis + (dayOffset * MILLIS_PER_DAY)
            candidate.set(Calendar.HOUR_OF_DAY, settings.hourOfDay)
            candidate.set(Calendar.MINUTE, settings.minute)
            candidate.set(Calendar.SECOND, 0)
            candidate.set(Calendar.MILLISECOND, 0)
            if (candidate.get(Calendar.DAY_OF_WEEK) in settings.selectedDays && candidate.after(now)) {
                nextReminder = candidate.timeInMillis
                break
            }
        }
        return nextReminder
    }
}
