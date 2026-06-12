package com.oceanofmaya.intervalwalktrainer.home

import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
import com.oceanofmaya.intervalwalktrainer.R
import com.oceanofmaya.intervalwalktrainer.WeeklyGoalUiFormatter
import com.oceanofmaya.intervalwalktrainer.WorkoutRepository
import com.oceanofmaya.intervalwalktrainer.WorkoutSession
import kotlin.math.roundToInt

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
        val lastWorkoutFormatter = LastWorkoutInsightFormatter(view.context)
        val accentColor = accentColorProvider()
        val mutedColor = ContextCompat.getColor(view.context, R.color.text_secondary)
        val primaryTextColor = ContextCompat.getColor(view.context, R.color.text_primary)

        view.findViewById<TextView>(R.id.lastWorkoutInsightTitle).text =
            view.context.getString(R.string.title_last_workout)

        when (val state = insightState) {
            InsightState.Disabled, InsightState.LoadError -> bindDisabledState(
                view = view,
                mutedColor = mutedColor
            )
            is InsightState.Active -> bindActiveState(
                view = view,
                session = state.session,
                formatter = formatter,
                lastWorkoutFormatter = lastWorkoutFormatter,
                accentColor = accentColor,
                primaryTextColor = primaryTextColor,
                secondaryTextColor = mutedColor
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
        mutedColor: Int
    ) {
        val emptyMessage = if (saveWorkoutsEnabledProvider()) {
            view.context.getString(R.string.body_last_workout_empty)
        } else {
            view.context.getString(R.string.body_last_workout_save_off)
        }

        view.findViewById<MaterialCardView>(R.id.lastWorkoutInsightCard).alpha = DISABLED_CARD_ALPHA
        view.findViewById<TextView>(R.id.lastWorkoutInsightTitle).setTextColor(mutedColor)
        view.findViewById<View>(R.id.lastWorkoutInsightActiveContent).visibility = View.GONE
        view.findViewById<View>(R.id.lastWorkoutInsightEmptyContent).visibility = View.VISIBLE
        HomeInsightCircleBinder.bindStatusCircle(
            statusRoot = view.findViewById(R.id.lastWorkoutInsightStatusCircle),
            iconResId = R.drawable.outline_directions_walk_24,
            iconTintColor = mutedColor,
            accessibilityLabel = view.context.getString(R.string.desc_home_last_workout_insight_empty),
            formatter = WeeklyGoalUiFormatter(view.context)
        )
        view.findViewById<TextView>(R.id.lastWorkoutInsightEmptyMessage).apply {
            text = emptyMessage
            setTextColor(mutedColor)
        }
        latestContentDescription = view.context.getString(R.string.desc_home_last_workout_insight_empty)
    }

    private fun bindActiveState(
        view: View,
        session: WorkoutSession,
        formatter: HomeInsightUiFormatter,
        lastWorkoutFormatter: LastWorkoutInsightFormatter,
        accentColor: Int,
        primaryTextColor: Int,
        secondaryTextColor: Int
    ) {
        view.findViewById<MaterialCardView>(R.id.lastWorkoutInsightCard).alpha = 1f
        view.findViewById<TextView>(R.id.lastWorkoutInsightTitle).setTextColor(primaryTextColor)
        view.findViewById<View>(R.id.lastWorkoutInsightEmptyContent).visibility = View.GONE
        view.findViewById<View>(R.id.lastWorkoutInsightActiveContent).visibility = View.VISIBLE

        val context = view.context
        HomeInsightCircleBinder.bindFlatStatCircle(
            statRoot = view.findViewById(R.id.lastWorkoutInsightMinutesCircle),
            valueText = formatter.flatStatMinutesValue(session.minutes),
            captionText = context.getString(R.string.label_minutes_short),
            valueTextColor = accentColor,
            captionTextColor = accentColor,
            formatter = formatter
        )
        view.findViewById<TextView>(R.id.lastWorkoutInsightFormula).apply {
            text = session.workoutType
            setTextColor(primaryTextColor)
        }
        view.findViewById<TextView>(R.id.lastWorkoutInsightWhen).apply {
            text = lastWorkoutFormatter.lastWorkoutWhenText(session)
            setTextColor(secondaryTextColor)
            bindLineStartIcon(
                iconResId = R.drawable.outline_timer_24,
                iconTintColor = secondaryTextColor
            )
        }

        latestContentDescription = context.getString(
            R.string.desc_home_last_workout_insight,
            session.workoutType,
            lastWorkoutFormatter.lastWorkoutMetaText(session)
        )
    }

    private fun TextView.bindLineStartIcon(
        iconResId: Int,
        iconTintColor: Int
    ) {
        val density = context.resources.displayMetrics.density
        val iconSize = (LINE_ICON_SIZE_DP * density).roundToInt()
        val icon = ContextCompat.getDrawable(context, iconResId)
            ?.mutate()
            ?.apply {
                setBounds(0, 0, iconSize, iconSize)
                setTint(iconTintColor)
            }
        includeFontPadding = false
        setCompoundDrawables(icon, null, null, null)
        compoundDrawablePadding = (LINE_ICON_PADDING_DP * density).roundToInt()
    }

    companion object {
        private const val TAG = "LastWorkoutHomeInsight"
        private const val DISABLED_CARD_ALPHA = 0.72f
        private const val LINE_ICON_SIZE_DP = 14
        private const val LINE_ICON_PADDING_DP = 4
    }
}
