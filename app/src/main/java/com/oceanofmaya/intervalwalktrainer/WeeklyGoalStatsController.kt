package com.oceanofmaya.intervalwalktrainer

import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.oceanofmaya.intervalwalktrainer.databinding.ActivityStatsBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt

class WeeklyGoalStatsController(
    private val activity: AppCompatActivity,
    private val binding: ActivityStatsBinding,
    private val workoutRepository: WorkoutRepository,
    sharedPreferences: SharedPreferences,
    private val accentColorProvider: () -> Int
) {
    private val editor = WeeklyGoalEditor(
        activity = activity,
        sharedPreferences = sharedPreferences,
        accentColorProvider = accentColorProvider,
        onSaved = ::load
    )
    private val sharedPreferences = sharedPreferences

    fun setup() {
        binding.editWeeklyGoalButton.setOnClickListener { view ->
            view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            editor.show()
        }
        binding.weeklyGoalCard.setOnClickListener { view ->
            view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            editor.show()
        }
        binding.weeklyGoalWorkoutsProgress.progressTintList =
            android.content.res.ColorStateList.valueOf(accentColorProvider())
        binding.weeklyGoalMinutesProgress.progressTintList =
            android.content.res.ColorStateList.valueOf(accentColorProvider())
    }

    fun load() {
        activity.lifecycleScope.launch {
            runCatching {
                val settings = WeeklyGoalPreferences.loadGoalSettings(sharedPreferences)
                val reminderSettings = WeeklyGoalPreferences.loadReminderSettings(sharedPreferences)
                if (!settings.enabled || !settings.hasAnyTarget) {
                    showDisabled(reminderSettings)
                } else {
                    showProgress(workoutRepository.getWeeklyGoalProgress(settings), reminderSettings)
                }
            }.onFailure { throwable ->
                android.util.Log.e(TAG, "Error loading weekly goal", throwable)
                binding.weeklyGoalStatus.text = activity.getString(R.string.body_weekly_goal_empty)
            }
        }
    }

    fun applyEmptyUi() {
        binding.weeklyGoalStatus.text = activity.getString(R.string.body_weekly_goal_empty)
        binding.weeklyGoalStatusPill.text = activity.getString(R.string.label_weekly_goal_on_track)
        styleStatusPill(accentColorProvider())
        binding.weeklyGoalRemainingText.visibility = View.GONE
        binding.weeklyGoalWorkoutsProgress.progress = 0
        binding.weeklyGoalMinutesProgress.progress = 0
    }

    private fun showDisabled(reminderSettings: WeeklyReminderSettings) {
        binding.weeklyGoalStatus.text = activity.getString(R.string.body_weekly_goal_off)
        binding.weeklyGoalStatusPill.text = activity.getString(R.string.label_weekly_goal_off)
        styleStatusPill(ContextCompat.getColor(activity, R.color.text_secondary))
        binding.weeklyGoalRemainingText.visibility = View.GONE
        binding.weeklyGoalWorkoutsProgress.visibility = View.GONE
        binding.weeklyGoalWorkoutsText.visibility = View.GONE
        binding.weeklyGoalMinutesProgress.visibility = View.GONE
        binding.weeklyGoalMinutesText.visibility = View.GONE
        binding.weeklyGoalReminderText.text = reminderSummary(reminderSettings, goalEnabled = false)
    }

    private fun showProgress(
        progress: WeeklyGoalProgress,
        reminderSettings: WeeklyReminderSettings
    ) {
        binding.weeklyGoalStatus.text = if (progress.isGoalMet) {
            activity.getString(R.string.body_weekly_goal_met)
        } else {
            activity.getString(R.string.body_weekly_goal_in_progress)
        }
        binding.weeklyGoalStatusPill.text = if (progress.isGoalMet) {
            activity.getString(R.string.label_weekly_goal_met)
        } else if (isLastGoalDay(progress)) {
            activity.getString(R.string.label_weekly_goal_last_day)
        } else {
            activity.getString(R.string.label_weekly_goal_on_track)
        }
        styleStatusPill(accentColorProvider())
        binding.weeklyGoalRemainingText.text = remainingText(progress)
        binding.weeklyGoalRemainingText.visibility = View.VISIBLE
        showWorkoutProgress(progress)
        showMinutesProgress(progress)
        binding.weeklyGoalReminderText.text = reminderSummary(reminderSettings, goalEnabled = true)
    }

    private fun showWorkoutProgress(progress: WeeklyGoalProgress) {
        if (progress.settings.tracksWorkouts) {
            binding.weeklyGoalWorkoutsProgress.visibility = View.VISIBLE
            binding.weeklyGoalWorkoutsText.visibility = View.VISIBLE
            binding.weeklyGoalWorkoutsProgress.progress = progress.workoutPercent
            binding.weeklyGoalWorkoutsText.text = activity.getString(
                R.string.format_weekly_goal_workouts_progress,
                progress.completedWorkouts,
                progress.settings.targetWorkouts
            )
        } else {
            binding.weeklyGoalWorkoutsProgress.visibility = View.GONE
            binding.weeklyGoalWorkoutsText.visibility = View.GONE
        }
    }

    private fun showMinutesProgress(progress: WeeklyGoalProgress) {
        if (progress.settings.tracksMinutes) {
            binding.weeklyGoalMinutesProgress.visibility = View.VISIBLE
            binding.weeklyGoalMinutesText.visibility = View.VISIBLE
            binding.weeklyGoalMinutesProgress.progress = progress.minutesPercent
            binding.weeklyGoalMinutesText.text = activity.getString(
                R.string.format_weekly_goal_minutes_progress,
                progress.completedMinutes,
                progress.settings.targetMinutes
            )
        } else {
            binding.weeklyGoalMinutesProgress.visibility = View.GONE
            binding.weeklyGoalMinutesText.visibility = View.GONE
        }
    }

    private fun remainingText(progress: WeeklyGoalProgress): String {
        if (progress.isGoalMet) {
            return activity.getString(R.string.body_weekly_goal_all_set)
        }
        val remainingParts = listOfNotNull(
            workoutsRemainingText(progress),
            minutesRemainingText(progress)
        )
        return when (remainingParts.size) {
            0 -> activity.getString(R.string.body_weekly_goal_all_set)
            1 -> activity.getString(R.string.format_weekly_goal_remaining_single, remainingParts[0])
            else -> activity.getString(
                R.string.format_weekly_goal_remaining_combined,
                remainingParts[0],
                remainingParts[1]
            )
        }
    }

    private fun workoutsRemainingText(progress: WeeklyGoalProgress): String? {
        val remaining = if (progress.settings.tracksWorkouts) {
            (progress.settings.targetWorkouts - progress.completedWorkouts).coerceAtLeast(0)
        } else {
            0
        }
        return remaining.takeIf { it > 0 }?.let { workoutsRemaining ->
            activity.resources.getQuantityString(
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
            activity.resources.getQuantityString(
                R.plurals.format_weekly_goal_minutes_remaining,
                minutesRemaining,
                minutesRemaining
            )
        }
    }

    private fun styleStatusPill(color: Int) {
        binding.weeklyGoalStatusPill.setTextColor(color)
        binding.weeklyGoalStatusPill.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dpToPx(PILL_CORNER_RADIUS_DP).toFloat()
            setColor(colorWithAlpha(color, PILL_BACKGROUND_ALPHA))
            setStroke(dpToPx(PILL_STROKE_WIDTH_DP), colorWithAlpha(color, PILL_STROKE_ALPHA))
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
        return (dp * activity.resources.displayMetrics.density).roundToInt()
    }

    private fun isLastGoalDay(progress: WeeklyGoalProgress): Boolean {
        val weekEndMillis = runCatching {
            SimpleDateFormat(WEEK_DATE_PATTERN, Locale.US).parse(progress.dateRange.endDate)?.time
        }.getOrNull() ?: return false
        val weekEndExclusive = Calendar.getInstance().apply {
            timeInMillis = weekEndMillis
            add(Calendar.DAY_OF_MONTH, 1)
        }.timeInMillis
        val remainingMillis = weekEndExclusive - System.currentTimeMillis()
        return remainingMillis in 1..MILLIS_PER_DAY
    }

    private fun reminderSummary(settings: WeeklyReminderSettings, goalEnabled: Boolean): String {
        return when {
            !goalEnabled -> activity.getString(R.string.body_weekly_goal_reminders_need_goal)
            !settings.enabled -> activity.getString(R.string.body_weekly_goal_reminders_off)
            else -> activity.getString(
                R.string.format_weekly_goal_reminders_on,
                formatReminderTime(settings.hourOfDay, settings.minute),
                settings.selectedDays.size
            )
        }
    }

    private fun formatReminderTime(hourOfDay: Int, minute: Int): String {
        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, hourOfDay)
            set(java.util.Calendar.MINUTE, minute)
        }
        return java.text.SimpleDateFormat("h:mm a", java.util.Locale.US).format(calendar.time)
    }

    companion object {
        private const val TAG = "WeeklyGoalStats"
        private const val PILL_CORNER_RADIUS_DP = 12
        private const val PILL_STROKE_WIDTH_DP = 1
        private const val PILL_BACKGROUND_ALPHA = 0.14f
        private const val PILL_STROKE_ALPHA = 0.35f
        private const val COLOR_CHANNEL_MAX = 255
        private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
        private const val WEEK_DATE_PATTERN = "yyyy-MM-dd"
    }
}
