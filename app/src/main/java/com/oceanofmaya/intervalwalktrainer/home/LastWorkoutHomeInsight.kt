package com.oceanofmaya.intervalwalktrainer.home

import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.oceanofmaya.intervalwalktrainer.R
import com.oceanofmaya.intervalwalktrainer.WorkoutRepository
import com.oceanofmaya.intervalwalktrainer.WorkoutSession

class LastWorkoutHomeInsight(
    private val workoutRepository: WorkoutRepository,
    private val saveWorkoutsEnabledProvider: () -> Boolean,
    private val accentColorProvider: () -> Int,
    private val onOpenWorkoutHistory: () -> Unit,
    private val onOpenWorkoutDate: (String) -> Unit
) : HomeInsightCard {
    override val id: String = HomeInsightCardIds.LAST_WORKOUT
    override val settingsLabelResId: Int = R.string.title_last_workout
    override val settingsDescriptionResId: Int = R.string.body_insight_card_last_workout_description
    override val layoutResId: Int = R.layout.home_insight_card_last_workout

    private sealed class InsightState {
        data object Disabled : InsightState()
        data class Active(val session: WorkoutSession) : InsightState()
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
            val session = workoutRepository.getLatestSession()
            if (session != null) {
                InsightState.Active(session)
            } else {
                InsightState.Disabled
            }
        }.getOrElse {
            android.util.Log.e(TAG, "Error loading last workout insight", it)
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

        view.findViewById<TextView>(R.id.lastWorkoutInsightTitle).text =
            view.context.getString(R.string.title_last_workout)

        when (val state = insightState) {
            InsightState.Disabled, InsightState.LoadError -> bindDisabledState(
                view = view,
                formatter = formatter,
                mutedColor = mutedColor
            )
            is InsightState.Active -> bindActiveState(
                view = view,
                session = state.session,
                formatter = formatter,
                accentColor = accentColor,
                primaryTextColor = primaryTextColor,
                secondaryTextColor = secondaryTextColor
            )
        }

        view.setOnClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            when (val state = insightState) {
                is InsightState.Active -> onOpenWorkoutDate(state.session.date)
                else -> onOpenWorkoutHistory()
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
        view.findViewById<MaterialCardView>(R.id.lastWorkoutInsightCard).alpha = DISABLED_CARD_ALPHA
        view.findViewById<TextView>(R.id.lastWorkoutInsightTitle).setTextColor(mutedColor)
        HomeInsightCircleBinder.bindFlatStatCircle(
            statRoot = view.findViewById(R.id.lastWorkoutInsightMinutesCircle),
            valueText = formatter.flatStatMinutesValue(0),
            captionText = view.context.getString(R.string.label_minutes_short),
            valueTextColor = mutedColor,
            captionTextColor = mutedColor,
            formatter = formatter
        )
        view.findViewById<TextView>(R.id.lastWorkoutInsightFormula).apply {
            text = view.context.getString(R.string.body_last_workout_empty)
            setTextColor(mutedColor)
        }
        view.findViewById<TextView>(R.id.lastWorkoutInsightDate).visibility = View.GONE
        latestContentDescription = view.context.getString(R.string.desc_home_last_workout_insight_empty)
    }

    private fun bindActiveState(
        view: View,
        session: WorkoutSession,
        formatter: HomeInsightUiFormatter,
        accentColor: Int,
        primaryTextColor: Int,
        secondaryTextColor: Int
    ) {
        val relativeDate = formatter.relativeDateLabel(session.date)
        view.findViewById<MaterialCardView>(R.id.lastWorkoutInsightCard).alpha = 1f
        view.findViewById<TextView>(R.id.lastWorkoutInsightTitle).setTextColor(primaryTextColor)
        HomeInsightCircleBinder.bindFlatStatCircle(
            statRoot = view.findViewById(R.id.lastWorkoutInsightMinutesCircle),
            valueText = formatter.flatStatMinutesValue(session.minutes),
            captionText = view.context.getString(R.string.label_minutes_short),
            valueTextColor = accentColor,
            captionTextColor = accentColor,
            formatter = formatter
        )
        view.findViewById<TextView>(R.id.lastWorkoutInsightFormula).apply {
            text = session.workoutType
            setTextColor(primaryTextColor)
        }
        view.findViewById<TextView>(R.id.lastWorkoutInsightDate).apply {
            visibility = View.VISIBLE
            text = relativeDate
            setTextColor(secondaryTextColor)
        }
        latestContentDescription = view.context.getString(
            R.string.desc_home_last_workout_insight,
            session.workoutType,
            view.context.getString(
                R.string.format_last_workout_subtitle,
                relativeDate,
                formatter.formatMinutes(session.minutes)
            )
        )
    }

    companion object {
        private const val TAG = "LastWorkoutHomeInsight"
        private const val DISABLED_CARD_ALPHA = 0.72f
    }
}
