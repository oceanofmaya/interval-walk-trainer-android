package com.oceanofmaya.intervalwalktrainer

import android.content.SharedPreferences
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.oceanofmaya.intervalwalktrainer.databinding.ActivityStatsBinding
import kotlinx.coroutines.launch

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
    private val formatter = WeeklyGoalUiFormatter(activity)

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
                if (!settings.remindersAvailable()) {
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
        formatter.styleStatusPill(binding.weeklyGoalStatusPill, accentColorProvider())
        binding.weeklyGoalRemainingText.visibility = View.GONE
        binding.weeklyGoalWorkoutsProgress.progress = 0
        binding.weeklyGoalMinutesProgress.progress = 0
    }

    private fun showDisabled(reminderSettings: WeeklyReminderSettings) {
        binding.weeklyGoalStatus.text = activity.getString(R.string.body_weekly_goal_off)
        binding.weeklyGoalStatusPill.text = activity.getString(R.string.label_weekly_goal_off)
        formatter.styleStatusPill(
            binding.weeklyGoalStatusPill,
            ContextCompat.getColor(activity, R.color.text_secondary)
        )
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
        binding.weeklyGoalStatus.text = formatter.statsStatusText(progress)
        binding.weeklyGoalStatusPill.text = formatter.statusPillText(progress)
        formatter.styleStatusPill(binding.weeklyGoalStatusPill, accentColorProvider())
        binding.weeklyGoalRemainingText.text = formatter.remainingText(progress)
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
            binding.weeklyGoalWorkoutsText.text = formatter.workoutsProgressLabel(progress)
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
            binding.weeklyGoalMinutesText.text = formatter.minutesProgressLabel(progress)
        } else {
            binding.weeklyGoalMinutesProgress.visibility = View.GONE
            binding.weeklyGoalMinutesText.visibility = View.GONE
        }
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
    }
}
