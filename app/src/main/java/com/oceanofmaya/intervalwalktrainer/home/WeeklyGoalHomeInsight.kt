package com.oceanofmaya.intervalwalktrainer.home

import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.google.android.material.card.MaterialCardView
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

    private sealed class InsightState {
        data object Disabled : InsightState()
        data class Active(val progress: WeeklyGoalProgress) : InsightState()
        data object LoadError : InsightState()
    }

    private var insightState: InsightState = InsightState.Disabled
    private var latestContentDescription: String = ""

    override suspend fun isEligible(): Boolean {
        val settings = WeeklyGoalPreferences.loadGoalSettings(sharedPreferences)
        if (!settings.enabled || !settings.hasAnyTarget) {
            insightState = InsightState.Disabled
            latestContentDescription = ""
            return true
        }

        insightState = runCatching {
            InsightState.Active(workoutRepository.getWeeklyGoalProgress(settings))
        }.getOrElse {
            android.util.Log.e(TAG, "Error loading weekly goal insight", it)
            InsightState.LoadError
        }
        latestContentDescription = ""
        return true
    }

    override fun bind(view: View) {
        val formatter = WeeklyGoalUiFormatter(view.context)
        val accentColor = accentColorProvider()

        view.findViewById<TextView>(R.id.weeklyGoalInsightTitle).text =
            view.context.getString(R.string.title_weekly_goal)

        when (val state = insightState) {
            InsightState.Disabled -> bindMutedPlaceholderState(
                view = view,
                contentDescription = formatter.homeInactiveContentDescription()
            )
            is InsightState.Active -> bindActiveState(view, state.progress, formatter, accentColor)
            InsightState.LoadError -> bindLoadErrorState(view)
        }

        view.setOnClickListener {
            it.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            onEditWeeklyGoal()
        }
        view.contentDescription = latestContentDescription
    }

    override fun contentDescription(): String = latestContentDescription

    private fun bindMutedPlaceholderState(view: View, contentDescription: String) {
        val context = view.context
        val mutedColor = ContextCompat.getColor(context, R.color.text_secondary)
        val zeroFraction = context.getString(R.string.format_weekly_goal_fraction, 0, 0)

        view.findViewById<MaterialCardView>(R.id.weeklyGoalInsightCard).alpha = DISABLED_CARD_ALPHA
        view.findViewById<TextView>(R.id.weeklyGoalInsightTitle)
            .setTextColor(mutedColor)
        setStatusMessageVisible(view, visible = false)
        view.findViewById<View>(R.id.weeklyGoalInsightRingsRow).visibility = View.VISIBLE

        bindStatusCircle(
            statusRoot = view.findViewById(R.id.weeklyGoalInsightStatusCircle),
            isGoalMet = false,
            accessibilityLabel = context.getString(R.string.label_weekly_goal_off),
            accentColor = mutedColor,
            formatter = WeeklyGoalUiFormatter(context)
        )
        bindRing(
            ringRoot = view.findViewById(R.id.weeklyGoalInsightWorkoutsRing),
            visible = true,
            percent = 0,
            fractionText = zeroFraction,
            captionText = context.getString(R.string.label_workouts_short),
            progressLabel = context.getString(R.string.format_weekly_goal_workouts_progress, 0, 0),
            accentColor = mutedColor,
            muted = true
        )
        bindRing(
            ringRoot = view.findViewById(R.id.weeklyGoalInsightMinutesRing),
            visible = true,
            percent = 0,
            fractionText = zeroFraction,
            captionText = context.getString(R.string.label_minutes_short),
            progressLabel = context.getString(R.string.format_weekly_goal_minutes_progress, 0, 0),
            accentColor = mutedColor,
            muted = true
        )

        latestContentDescription = contentDescription
    }

    private fun bindLoadErrorState(view: View) {
        val context = view.context
        val primaryTextColor = ContextCompat.getColor(context, R.color.text_primary)
        val message = context.getString(R.string.body_weekly_goal_empty)

        view.findViewById<MaterialCardView>(R.id.weeklyGoalInsightCard).alpha = 1f
        view.findViewById<TextView>(R.id.weeklyGoalInsightTitle)
            .setTextColor(primaryTextColor)
        setStatusMessageVisible(view, message = message)
        view.findViewById<View>(R.id.weeklyGoalInsightRingsRow).visibility = View.GONE
        latestContentDescription = message
    }

    private fun bindActiveState(
        view: View,
        progress: WeeklyGoalProgress,
        formatter: WeeklyGoalUiFormatter,
        accentColor: Int
    ) {
        val context = view.context
        val primaryTextColor = ContextCompat.getColor(context, R.color.text_primary)
        val secondaryTextColor = ContextCompat.getColor(context, R.color.text_secondary)

        view.findViewById<MaterialCardView>(R.id.weeklyGoalInsightCard).alpha = 1f
        view.findViewById<TextView>(R.id.weeklyGoalInsightTitle)
            .setTextColor(primaryTextColor)
        setStatusMessageVisible(view, visible = false)
        view.findViewById<View>(R.id.weeklyGoalInsightRingsRow).visibility = View.VISIBLE

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
            captionText = context.getString(R.string.label_workouts_short),
            progressLabel = formatter.workoutsProgressLabel(progress),
            accentColor = accentColor,
            muted = false,
            fractionTextColor = primaryTextColor,
            captionTextColor = secondaryTextColor
        )
        bindRing(
            ringRoot = view.findViewById(R.id.weeklyGoalInsightMinutesRing),
            visible = progress.settings.tracksMinutes,
            percent = progress.minutesPercent,
            fractionText = formatter.minutesFractionText(progress),
            captionText = context.getString(R.string.label_minutes_short),
            progressLabel = formatter.minutesProgressLabel(progress),
            accentColor = accentColor,
            muted = false,
            fractionTextColor = primaryTextColor,
            captionTextColor = secondaryTextColor
        )
    }

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
        accentColor: Int,
        muted: Boolean,
        fractionTextColor: Int = ContextCompat.getColor(ringRoot.context, R.color.text_primary),
        captionTextColor: Int = ContextCompat.getColor(ringRoot.context, R.color.text_secondary)
    ) {
        ringRoot.visibility = if (visible) View.VISIBLE else View.GONE
        if (!visible) {
            return
        }

        val indicator = ringRoot.findViewById<CircularProgressIndicator>(R.id.weeklyGoalRingProgress)
        val fractionView = ringRoot.findViewById<TextView>(R.id.weeklyGoalRingFraction)
        val captionView = ringRoot.findViewById<TextView>(R.id.weeklyGoalRingCaption)

        fractionView.text = fractionText
        captionView.text = captionText
        fractionView.setTextColor(if (muted) accentColor else fractionTextColor)
        captionView.setTextColor(if (muted) accentColor else captionTextColor)
        indicator.setIndicatorColor(accentColor)
        indicator.setProgressCompat(percent, false)
        ringRoot.contentDescription = progressLabel
    }

    private fun setStatusMessageVisible(
        view: View,
        visible: Boolean = true,
        message: String? = null
    ) {
        val statusMessage = view.findViewById<TextView>(R.id.weeklyGoalInsightStatusMessage)
        if (!visible) {
            statusMessage.visibility = View.GONE
            return
        }
        statusMessage.text = message.orEmpty()
        statusMessage.visibility = View.VISIBLE
    }

    companion object {
        private const val TAG = "WeeklyGoalHomeInsight"
        private const val DISABLED_CARD_ALPHA = 0.72f
    }
}
