package com.oceanofmaya.intervalwalktrainer.home

import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.oceanofmaya.intervalwalktrainer.R
import com.oceanofmaya.intervalwalktrainer.WeeklyGoalUiFormatter
import com.oceanofmaya.intervalwalktrainer.WorkoutRepository
import com.oceanofmaya.intervalwalktrainer.WorkoutStatistics

class CurrentStreakHomeInsight(
    private val workoutRepository: WorkoutRepository,
    private val saveWorkoutsEnabledProvider: () -> Boolean,
    private val accentColorProvider: () -> Int,
    private val onOpenWorkoutHistory: () -> Unit
) : HomeInsightCard {
    override val id: String = HomeInsightCardIds.CURRENT_STREAK
    override val settingsLabelResId: Int = R.string.label_current_streak
    override val settingsDescriptionResId: Int = R.string.body_insight_card_current_streak_description
    override val layoutResId: Int = R.layout.home_insight_card_current_streak

    private sealed class InsightState {
        data object Disabled : InsightState()
        data class Active(val statistics: WorkoutStatistics) : InsightState()
        data object LoadError : InsightState()
    }

    private var insightState: InsightState = InsightState.Disabled
    private var latestContentDescription: String = ""

    override suspend fun isEligible(): Boolean {
        if (!saveWorkoutsEnabledProvider()) {
            insightState = InsightState.Disabled
            latestContentDescription = ""
            return true
        }

        insightState = runCatching {
            val statistics = workoutRepository.getStatistics()
            if (statistics.totalWorkouts == 0) {
                InsightState.Disabled
            } else {
                InsightState.Active(statistics)
            }
        }.getOrElse {
            android.util.Log.e(TAG, "Error loading current streak insight", it)
            InsightState.LoadError
        }
        latestContentDescription = ""
        return true
    }

    override fun bind(view: View) {
        val formatter = HomeInsightUiFormatter(view.context)
        val accentColor = accentColorProvider()
        val mutedColor = ContextCompat.getColor(view.context, R.color.text_secondary)
        val primaryTextColor = ContextCompat.getColor(view.context, R.color.text_primary)
        val secondaryTextColor = ContextCompat.getColor(view.context, R.color.text_secondary)

        view.findViewById<TextView>(R.id.currentStreakInsightTitle).text =
            view.context.getString(R.string.label_current_streak)

        when (val state = insightState) {
            InsightState.Disabled, InsightState.LoadError -> bindDisabledState(
                view = view,
                formatter = formatter,
                mutedColor = mutedColor,
                secondaryTextColor = secondaryTextColor
            )
            is InsightState.Active -> bindActiveState(
                view = view,
                statistics = state.statistics,
                formatter = formatter,
                accentColor = accentColor,
                mutedColor = mutedColor,
                primaryTextColor = primaryTextColor,
                secondaryTextColor = secondaryTextColor
            )
        }

        view.setOnClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            onOpenWorkoutHistory()
        }
        view.contentDescription = latestContentDescription
    }

    override fun contentDescription(): String = latestContentDescription

    private fun bindDisabledState(
        view: View,
        formatter: HomeInsightUiFormatter,
        mutedColor: Int,
        secondaryTextColor: Int
    ) {
        view.findViewById<MaterialCardView>(R.id.currentStreakInsightCard).alpha = DISABLED_CARD_ALPHA
        view.findViewById<TextView>(R.id.currentStreakInsightTitle).setTextColor(mutedColor)
        bindVisuals(
            view = view,
            formatter = formatter,
            statusFormatter = WeeklyGoalUiFormatter(view.context),
            currentStreak = 0,
            longestStreak = 0,
            iconTintColor = mutedColor,
            valueTextColor = mutedColor,
            captionTextColor = mutedColor,
            secondaryTextColor = secondaryTextColor,
            showBest = false
        )
        latestContentDescription = view.context.getString(R.string.desc_home_current_streak_insight_off)
    }

    private fun bindActiveState(
        view: View,
        statistics: WorkoutStatistics,
        formatter: HomeInsightUiFormatter,
        accentColor: Int,
        mutedColor: Int,
        primaryTextColor: Int,
        secondaryTextColor: Int
    ) {
        view.findViewById<MaterialCardView>(R.id.currentStreakInsightCard).alpha = 1f
        view.findViewById<TextView>(R.id.currentStreakInsightTitle)
            .setTextColor(primaryTextColor)
        val discTextColor = if (statistics.currentStreak > 0) accentColor else mutedColor
        bindVisuals(
            view = view,
            formatter = formatter,
            statusFormatter = WeeklyGoalUiFormatter(view.context),
            currentStreak = statistics.currentStreak,
            longestStreak = statistics.longestStreak,
            iconTintColor = discTextColor,
            valueTextColor = discTextColor,
            captionTextColor = discTextColor,
            secondaryTextColor = secondaryTextColor,
            showBest = statistics.longestStreak > 0
        )

        val bestLabel = if (statistics.longestStreak > 0) {
            formatter.streakBestLabel(statistics.longestStreak)
        } else {
            ""
        }
        latestContentDescription = view.context.getString(
            R.string.desc_home_current_streak_insight,
            formatter.streakDaysText(statistics.currentStreak),
            bestLabel
        )
    }

    private fun bindVisuals(
        view: View,
        formatter: HomeInsightUiFormatter,
        statusFormatter: WeeklyGoalUiFormatter,
        currentStreak: Int,
        longestStreak: Int,
        iconTintColor: Int,
        valueTextColor: Int,
        captionTextColor: Int,
        secondaryTextColor: Int,
        showBest: Boolean
    ) {
        HomeInsightCircleBinder.bindStatusCircle(
            statusRoot = view.findViewById(R.id.currentStreakInsightStatusCircle),
            iconResId = R.drawable.baseline_flash_on_24,
            iconTintColor = iconTintColor,
            accessibilityLabel = view.context.getString(R.string.label_current_streak),
            formatter = statusFormatter
        )
        HomeInsightCircleBinder.bindFlatStatCircle(
            statRoot = view.findViewById(R.id.currentStreakInsightDaysCircle),
            valueText = formatter.flatStatStreakValue(currentStreak),
            captionText = view.context.getString(R.string.label_days_short),
            valueTextColor = valueTextColor,
            captionTextColor = captionTextColor,
            formatter = formatter
        )

        val bestText = view.findViewById<TextView>(R.id.currentStreakInsightBestText)
        if (showBest && longestStreak > 0) {
            bestText.visibility = View.VISIBLE
            bestText.text = formatter.streakBestLabel(longestStreak)
            bestText.setTextColor(secondaryTextColor)
        } else {
            bestText.visibility = View.GONE
        }
    }

    companion object {
        private const val TAG = "CurrentStreakHomeInsight"
        private const val DISABLED_CARD_ALPHA = 0.72f
    }
}
