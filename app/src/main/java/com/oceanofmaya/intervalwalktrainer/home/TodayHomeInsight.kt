package com.oceanofmaya.intervalwalktrainer.home

import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.oceanofmaya.intervalwalktrainer.R
import com.oceanofmaya.intervalwalktrainer.WeeklyGoalUiFormatter
import com.oceanofmaya.intervalwalktrainer.WorkoutRecord
import com.oceanofmaya.intervalwalktrainer.WorkoutRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TodayHomeInsight(
    private val workoutRepository: WorkoutRepository,
    private val saveWorkoutsEnabledProvider: () -> Boolean,
    private val accentColorProvider: () -> Int,
    private val onOpenWorkoutHistory: () -> Unit,
    private val onOpenWorkoutDate: (String) -> Unit
) : HomeInsightCard {
    override val id: String = HomeInsightCardIds.TODAY
    override val settingsLabelResId: Int = R.string.label_today
    override val settingsDescriptionResId: Int = R.string.body_insight_card_today_description
    override val layoutResId: Int = R.layout.home_insight_card_today

    private sealed class InsightState {
        data object Disabled : InsightState()
        data object Zero : InsightState()
        data class Active(val record: WorkoutRecord) : InsightState()
        data object LoadError : InsightState()
    }

    private var insightState: InsightState = InsightState.Disabled
    private var todayDate: String = ""
    private var latestContentDescription: String = ""

    override suspend fun isEligible(): Boolean {
        todayDate = dateFormat.format(Date())

        if (!saveWorkoutsEnabledProvider()) {
            insightState = InsightState.Disabled
            latestContentDescription = ""
            return true
        }

        insightState = runCatching {
            val record = workoutRepository.getRecordByDate(todayDate)
            if (record != null && record.completedWorkouts > 0) {
                InsightState.Active(record)
            } else {
                InsightState.Zero
            }
        }.getOrElse {
            android.util.Log.e(TAG, "Error loading today insight", it)
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

        view.findViewById<TextView>(R.id.todayInsightTitle).text =
            view.context.getString(R.string.label_today)

        when (val state = insightState) {
            InsightState.Disabled, InsightState.LoadError -> bindDisabledState(
                view = view,
                formatter = formatter,
                mutedColor = mutedColor
            )
            InsightState.Zero -> bindLiveState(
                view = view,
                formatter = formatter,
                workouts = 0,
                minutes = 0,
                accentColor = accentColor,
                mutedColor = mutedColor,
                primaryTextColor = primaryTextColor
            )
            is InsightState.Active -> bindLiveState(
                view = view,
                formatter = formatter,
                workouts = state.record.completedWorkouts,
                minutes = state.record.totalMinutes,
                accentColor = accentColor,
                mutedColor = mutedColor,
                primaryTextColor = primaryTextColor
            )
        }

        view.setOnClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            if (insightState is InsightState.Active) {
                onOpenWorkoutDate(todayDate)
            } else {
                onOpenWorkoutHistory()
            }
        }
        view.contentDescription = latestContentDescription
    }

    override fun contentDescription(): String = latestContentDescription

    private fun bindDisabledState(
        view: View,
        formatter: HomeInsightUiFormatter,
        mutedColor: Int
    ) {
        view.findViewById<MaterialCardView>(R.id.todayInsightCard).alpha = DISABLED_CARD_ALPHA
        view.findViewById<TextView>(R.id.todayInsightTitle).setTextColor(mutedColor)
        bindVisuals(
            view = view,
            formatter = formatter,
            statusFormatter = WeeklyGoalUiFormatter(view.context),
            workouts = 0,
            minutes = 0,
            iconTintColor = mutedColor,
            valueTextColor = mutedColor,
            captionTextColor = mutedColor
        )
        latestContentDescription = view.context.getString(R.string.desc_home_today_insight_empty)
    }

    private fun bindLiveState(
        view: View,
        formatter: HomeInsightUiFormatter,
        workouts: Int,
        minutes: Int,
        accentColor: Int,
        mutedColor: Int,
        primaryTextColor: Int
    ) {
        view.findViewById<MaterialCardView>(R.id.todayInsightCard).alpha = 1f
        view.findViewById<TextView>(R.id.todayInsightTitle).setTextColor(primaryTextColor)
        val discTextColor = if (workouts > 0) accentColor else mutedColor
        bindVisuals(
            view = view,
            formatter = formatter,
            statusFormatter = WeeklyGoalUiFormatter(view.context),
            workouts = workouts,
            minutes = minutes,
            iconTintColor = discTextColor,
            valueTextColor = discTextColor,
            captionTextColor = discTextColor
        )
        latestContentDescription = if (workouts > 0) {
            val summary = formatter.todaySummaryText(workouts, minutes)
            view.context.getString(R.string.desc_home_today_insight, summary)
        } else {
            view.context.getString(R.string.desc_home_today_insight_empty)
        }
    }

    private fun bindVisuals(
        view: View,
        formatter: HomeInsightUiFormatter,
        statusFormatter: WeeklyGoalUiFormatter,
        workouts: Int,
        minutes: Int,
        iconTintColor: Int,
        valueTextColor: Int,
        captionTextColor: Int
    ) {
        HomeInsightCircleBinder.bindStatusCircle(
            statusRoot = view.findViewById(R.id.todayInsightStatusCircle),
            iconResId = R.drawable.outline_today_24,
            iconTintColor = iconTintColor,
            accessibilityLabel = view.context.getString(R.string.label_today),
            formatter = statusFormatter
        )
        bindFlatStats(
            view = view,
            formatter = formatter,
            workouts = workouts,
            minutes = minutes,
            valueTextColor = valueTextColor,
            captionTextColor = captionTextColor
        )
    }

    private fun bindFlatStats(
        view: View,
        formatter: HomeInsightUiFormatter,
        workouts: Int,
        minutes: Int,
        valueTextColor: Int,
        captionTextColor: Int
    ) {
        val context = view.context
        HomeInsightCircleBinder.bindFlatStatCircle(
            statRoot = view.findViewById(R.id.todayInsightWorkoutsCircle),
            valueText = formatter.flatStatWorkoutsValue(workouts),
            captionText = context.getString(R.string.label_workouts_short),
            valueTextColor = valueTextColor,
            captionTextColor = captionTextColor,
            formatter = formatter
        )
        HomeInsightCircleBinder.bindFlatStatCircle(
            statRoot = view.findViewById(R.id.todayInsightMinutesCircle),
            valueText = formatter.flatStatMinutesValue(minutes),
            captionText = context.getString(R.string.label_minutes_short),
            valueTextColor = valueTextColor,
            captionTextColor = captionTextColor,
            formatter = formatter
        )
    }

    companion object {
        private const val TAG = "TodayHomeInsight"
        private const val DISABLED_CARD_ALPHA = 0.72f
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    }
}
