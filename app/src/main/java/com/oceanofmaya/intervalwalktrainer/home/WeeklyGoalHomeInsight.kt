package com.oceanofmaya.intervalwalktrainer.home

import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.oceanofmaya.intervalwalktrainer.R
import com.oceanofmaya.intervalwalktrainer.WeeklyGoalPreferences
import com.oceanofmaya.intervalwalktrainer.WeeklyGoalProgress
import com.oceanofmaya.intervalwalktrainer.WeeklyGoalUiFormatter
import com.oceanofmaya.intervalwalktrainer.WorkoutRepository

class WeeklyGoalHomeInsight(
    private val sharedPreferences: SharedPreferences,
    private val workoutRepository: WorkoutRepository,
    private val accentColorProvider: () -> Int,
    private val onEditWeeklyGoal: () -> Unit
) : HomeInsightCard {
    override val layoutResId: Int = R.layout.home_insight_card_weekly_goal

    private var latestProgress: WeeklyGoalProgress? = null
    private var latestContentDescription: String = ""

    override suspend fun isEligible(): Boolean {
        val settings = WeeklyGoalPreferences.loadGoalSettings(sharedPreferences)
        if (!settings.enabled || !settings.hasAnyTarget) {
            latestProgress = null
            latestContentDescription = ""
            return false
        }
        return runCatching {
            latestProgress = workoutRepository.getWeeklyGoalProgress(settings)
            true
        }.getOrElse {
            android.util.Log.e(TAG, "Error loading weekly goal insight", it)
            latestProgress = null
            latestContentDescription = ""
            false
        }
    }

    override fun bind(view: View) {
        val progress = latestProgress ?: return
        val formatter = WeeklyGoalUiFormatter(view.context)
        val accentColor = accentColorProvider()

        view.findViewById<TextView>(R.id.weeklyGoalInsightTitle).text =
            view.context.getString(R.string.title_weekly_goal)
        latestContentDescription = formatter.homeContentDescription(progress)

        bindStatusCircle(
            statusRoot = view.findViewById(R.id.weeklyGoalInsightStatusCircle),
            isGoalMet = progress.isGoalMet,
            accessibilityLabel = formatter.homeInsightStatusContentDescription(progress),
            accentColor = accentColor,
            formatter = formatter
        )
        bindRing(
            ringRoot = view.findViewById(R.id.weeklyGoalInsightWorkoutsRing),
            visible = progress.settings.tracksWorkouts,
            percent = progress.workoutPercent,
            fractionText = formatter.workoutsFractionText(progress),
            captionText = view.context.getString(R.string.label_workouts_short),
            progressLabel = formatter.workoutsProgressLabel(progress),
            accentColor = accentColor
        )
        bindRing(
            ringRoot = view.findViewById(R.id.weeklyGoalInsightMinutesRing),
            visible = progress.settings.tracksMinutes,
            percent = progress.minutesPercent,
            fractionText = formatter.minutesFractionText(progress),
            captionText = view.context.getString(R.string.label_minutes_short),
            progressLabel = formatter.minutesProgressLabel(progress),
            accentColor = accentColor
        )

        view.setOnClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            onEditWeeklyGoal()
        }
        view.contentDescription = latestContentDescription
    }

    override fun contentDescription(): String = latestContentDescription

    private fun bindStatusCircle(
        statusRoot: View,
        isGoalMet: Boolean,
        accessibilityLabel: String,
        accentColor: Int,
        formatter: WeeklyGoalUiFormatter
    ) {
        val iconView = statusRoot.findViewById<ImageView>(R.id.weeklyGoalStatusCircleIcon)
        if (isGoalMet) {
            iconView.setImageResource(R.drawable.baseline_check_24)
        } else {
            iconView.setImageResource(R.drawable.outline_directions_walk_24)
        }
        iconView.imageTintList = ColorStateList.valueOf(accentColor)
        formatter.styleStatusCircle(
            fillView = statusRoot.findViewById(R.id.weeklyGoalStatusCircleFill)
        )
        statusRoot.contentDescription = accessibilityLabel
    }

    private fun bindRing(
        ringRoot: View,
        visible: Boolean,
        percent: Int,
        fractionText: String,
        captionText: String,
        progressLabel: String,
        accentColor: Int
    ) {
        ringRoot.visibility = if (visible) View.VISIBLE else View.GONE
        if (!visible) {
            return
        }

        val indicator = ringRoot.findViewById<CircularProgressIndicator>(R.id.weeklyGoalRingProgress)
        ringRoot.findViewById<TextView>(R.id.weeklyGoalRingFraction).text = fractionText
        ringRoot.findViewById<TextView>(R.id.weeklyGoalRingCaption).text = captionText
        indicator.setIndicatorColor(accentColor)
        indicator.setProgressCompat(percent, false)
        ringRoot.contentDescription = progressLabel
    }

    companion object {
        private const val TAG = "WeeklyGoalHomeInsight"
    }
}
