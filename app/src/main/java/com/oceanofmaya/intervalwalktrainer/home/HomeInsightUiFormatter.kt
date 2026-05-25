package com.oceanofmaya.intervalwalktrainer.home

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.View
import com.oceanofmaya.intervalwalktrainer.R
import com.oceanofmaya.intervalwalktrainer.WorkoutSession
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HomeInsightUiFormatter(private val context: Context) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val displayDateFormat = SimpleDateFormat("MMM d", Locale.US)

    fun todayDateString(): String = dateFormat.format(Date())

    fun streakDaysText(days: Int): String {
        return if (days == 1) {
            context.getString(R.string.format_day, days)
        } else {
            context.getString(R.string.format_days, days)
        }
    }

    fun streakProgressPercent(currentStreak: Int, longestStreak: Int): Int {
        if (longestStreak <= 0) {
            return 0
        }
        return ((currentStreak.toFloat() / longestStreak.toFloat()) * 100)
            .toInt()
            .coerceIn(0, 100)
    }

    fun streakProgressLabel(currentStreak: Int, longestStreak: Int): String {
        return context.getString(R.string.format_streak_progress, currentStreak, longestStreak)
    }

    fun todaySummaryText(workouts: Int, minutes: Int): String {
        val workoutsText = if (workouts == 1) {
            context.getString(R.string.format_workout_count_singular)
        } else {
            context.getString(R.string.format_workout_count, workouts)
        }
        val minutesText = formatMinutes(minutes)
        return context.getString(R.string.format_today_summary, workoutsText, minutesText)
    }

    fun relativeDateLabel(date: String): String {
        val today = todayDateString()
        return when {
            date == today -> context.getString(R.string.label_today)
            date == yesterdayDateString() -> context.getString(R.string.label_yesterday)
            else -> runCatching {
                val parsed = dateFormat.parse(date)
                if (parsed != null) {
                    displayDateFormat.format(parsed)
                } else {
                    date
                }
            }.getOrDefault(date)
        }
    }

    private fun yesterdayDateString(): String {
        val yesterdayCalendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, -1)
        }
        return dateFormat.format(yesterdayCalendar.time)
    }

    fun lastWorkoutSubtitle(session: WorkoutSession): String {
        val relativeDate = relativeDateLabel(session.date)
        val minutesText = formatMinutes(session.minutes)
        return context.getString(R.string.format_last_workout_subtitle, relativeDate, minutesText)
    }

    fun formatMinutes(minutes: Int): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return if (hours > 0) {
            context.getString(R.string.format_time_hr_min, hours, mins)
        } else {
            context.getString(R.string.format_time_min, mins)
        }
    }

    fun isToday(date: String): Boolean = date == todayDateString()

    fun streakBestLabel(longestStreak: Int): String {
        return context.getString(R.string.format_streak_best, streakDaysText(longestStreak))
    }

    fun flatStatMinutesValue(minutes: Int): String = minutes.toString()

    fun flatStatWorkoutsValue(workouts: Int): String = workouts.toString()

    fun flatStatStreakValue(days: Int): String = days.toString()

    fun styleFlatStatCircle(fillView: View) {
        fillView.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(ContextCompat.getColor(context, R.color.home_insight_status_circle_fill))
            setStroke(
                dpToPx(CIRCLE_STROKE_WIDTH_DP),
                ContextCompat.getColor(context, R.color.stroke_light)
            )
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }

    companion object {
        private const val CIRCLE_STROKE_WIDTH_DP = 1
    }
}
