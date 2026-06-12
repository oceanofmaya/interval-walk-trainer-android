package com.oceanofmaya.intervalwalktrainer.home

import android.content.Context
import com.oceanofmaya.intervalwalktrainer.R
import com.oceanofmaya.intervalwalktrainer.WorkoutSession
import java.text.DateFormat
import java.util.Date
import java.util.Locale

class LastWorkoutInsightFormatter(private val context: Context) {
    private val baseFormatter = HomeInsightUiFormatter(context)

    fun lastWorkoutWhenText(session: WorkoutSession): String {
        val relativeDate = baseFormatter.relativeDateLabel(session.date)
        if (!shouldShowCompletionTime(session.date)) {
            return relativeDate
        }
        val timeText = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault())
            .format(Date(session.timestamp))
        return listOf(relativeDate, timeText)
            .joinToString(context.getString(R.string.separator_bullet))
    }

    fun lastWorkoutMetaText(session: WorkoutSession): String {
        return context.getString(
            R.string.format_last_workout_meta,
            baseFormatter.formatMinutes(session.minutes),
            lastWorkoutWhenText(session)
        )
    }

    private fun shouldShowCompletionTime(date: String): Boolean {
        return baseFormatter.isToday(date) || date == yesterdayDateString()
    }

    private fun yesterdayDateString(): String {
        val calendar = java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.DAY_OF_MONTH, -1)
        }
        return java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
    }
}
