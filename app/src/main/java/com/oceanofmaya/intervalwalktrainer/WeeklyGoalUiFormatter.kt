package com.oceanofmaya.intervalwalktrainer

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class WeeklyGoalUiFormatter(
    private val context: Context
) {
    fun statusPillText(progress: WeeklyGoalProgress): String {
        return when {
            progress.isGoalMet -> context.getString(R.string.label_weekly_goal_met)
            isLastGoalDay(progress) -> context.getString(R.string.label_weekly_goal_last_day)
            else -> context.getString(R.string.label_weekly_goal_on_track)
        }
    }

    fun homeInsightStatusContentDescription(progress: WeeklyGoalProgress): String {
        return statusPillText(progress)
    }

    fun workoutsProgressLabel(progress: WeeklyGoalProgress): String {
        return context.getString(
            R.string.format_weekly_goal_workouts_progress,
            progress.displayedCompletedWorkouts,
            progress.settings.targetWorkouts
        )
    }

    fun minutesProgressLabel(progress: WeeklyGoalProgress): String {
        return context.getString(
            R.string.format_weekly_goal_minutes_progress,
            progress.displayedCompletedMinutes,
            progress.settings.targetMinutes
        )
    }

    fun workoutsFractionText(progress: WeeklyGoalProgress): String {
        return context.getString(
            R.string.format_weekly_goal_fraction,
            progress.displayedCompletedWorkouts,
            progress.settings.targetWorkouts
        )
    }

    fun minutesFractionText(progress: WeeklyGoalProgress): String {
        return context.getString(
            R.string.format_weekly_goal_fraction,
            progress.displayedCompletedMinutes,
            progress.settings.targetMinutes
        )
    }

    fun homeInsightDetailText(progress: WeeklyGoalProgress): String {
        val metricLabels = buildList {
            if (progress.settings.tracksWorkouts) {
                add(workoutsProgressLabel(progress))
            }
            if (progress.settings.tracksMinutes) {
                add(minutesProgressLabel(progress))
            }
        }
        val status = statusPillText(progress)
        return if (metricLabels.isEmpty()) {
            status
        } else {
            context.getString(
                R.string.format_home_weekly_goal_insight_detail,
                status,
                metricLabels.joinToString(", ")
            )
        }
    }

    fun statsStatusText(progress: WeeklyGoalProgress): String {
        return if (progress.isGoalMet) {
            context.getString(R.string.body_weekly_goal_met)
        } else {
            context.getString(R.string.body_weekly_goal_in_progress)
        }
    }

    fun remainingText(progress: WeeklyGoalProgress): String {
        if (progress.isGoalMet) {
            return context.getString(R.string.body_weekly_goal_all_set)
        }
        val remainingParts = listOfNotNull(
            workoutsRemainingText(progress),
            minutesRemainingText(progress)
        )
        return when (remainingParts.size) {
            0 -> context.getString(R.string.body_weekly_goal_all_set)
            1 -> context.getString(R.string.format_weekly_goal_remaining_single, remainingParts[0])
            else -> context.getString(
                R.string.format_weekly_goal_remaining_combined,
                remainingParts[0],
                remainingParts[1]
            )
        }
    }

    fun homeContentDescription(progress: WeeklyGoalProgress): String {
        return context.getString(
            R.string.desc_home_weekly_goal_insight,
            homeInsightDetailText(progress)
        )
    }

    fun homeInactiveContentDescription(): String {
        return context.getString(R.string.desc_home_weekly_goal_insight_off)
    }

    fun styleStatusPill(view: TextView, color: Int) {
        view.setTextColor(color)
        view.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dpToPx(PILL_CORNER_RADIUS_DP).toFloat()
            setColor(colorWithAlpha(color, PILL_BACKGROUND_ALPHA))
            setStroke(dpToPx(PILL_STROKE_WIDTH_DP), colorWithAlpha(color, PILL_STROKE_ALPHA))
        }
    }

    fun styleStatusCircle(fillView: View) {
        fillView.background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(ContextCompat.getColor(context, R.color.home_insight_status_circle_fill))
            setStroke(
                dpToPx(STATUS_CIRCLE_STROKE_WIDTH_DP),
                ContextCompat.getColor(context, R.color.stroke_light)
            )
        }
    }

    fun isLastGoalDay(progress: WeeklyGoalProgress, nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (progress.isGoalMet) {
            return false
        }
        val today = SimpleDateFormat(WEEK_DATE_PATTERN, Locale.US).format(Date(nowMillis))
        return today == progress.dateRange.endDate
    }

    private fun workoutsRemainingText(progress: WeeklyGoalProgress): String? {
        val remaining = if (progress.settings.tracksWorkouts) {
            (progress.settings.targetWorkouts - progress.completedWorkouts).coerceAtLeast(0)
        } else {
            0
        }
        return remaining.takeIf { it > 0 }?.let { workoutsRemaining ->
            context.resources.getQuantityString(
                R.plurals.format_weekly_goal_workouts_remaining,
                workoutsRemaining,
                workoutsRemaining
            )
        }
    }

    private fun minutesRemainingText(progress: WeeklyGoalProgress): String? {
        val remaining = if (progress.settings.tracksMinutes) {
            (progress.settings.targetMinutes - progress.completedMinutes).coerceAtLeast(0)
        } else {
            0
        }
        return remaining.takeIf { it > 0 }?.let { minutesRemaining ->
            context.resources.getQuantityString(
                R.plurals.format_weekly_goal_minutes_remaining,
                minutesRemaining,
                minutesRemaining
            )
        }
    }

    private fun colorWithAlpha(color: Int, alpha: Float): Int {
        return Color.argb(
            (alpha * COLOR_CHANNEL_MAX).roundToInt(),
            Color.red(color),
            Color.green(color),
            Color.blue(color)
        )
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).roundToInt()
    }

    companion object {
        private const val PILL_CORNER_RADIUS_DP = 12
        private const val PILL_STROKE_WIDTH_DP = 1
        private const val STATUS_CIRCLE_STROKE_WIDTH_DP = 1
        private const val PILL_BACKGROUND_ALPHA = 0.14f
        private const val PILL_STROKE_ALPHA = 0.35f
        private const val COLOR_CHANNEL_MAX = 255
        private const val WEEK_DATE_PATTERN = "yyyy-MM-dd"
    }
}
