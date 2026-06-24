package com.oceanofmaya.intervalwalktrainer

import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.oceanofmaya.intervalwalktrainer.databinding.ActivityStatsBinding
import kotlinx.coroutines.launch
import kotlin.math.abs

class StatsTrendsController(
    private val activity: AppCompatActivity,
    private val binding: ActivityStatsBinding,
    private val workoutRepository: WorkoutRepository,
    private val accentColorProvider: () -> Int,
    private val formatMinutes: (Int) -> String,
    private val weeklyGoalSettingsProvider: () -> WeeklyGoalSettings,
    private val metricsEnabledProvider: () -> Boolean
) {
    fun loadMonthComparison(year: Int, month: Int) {
        activity.lifecycleScope.launch {
            runCatching {
                val settings = weeklyGoalSettingsProvider()
                MonthTrends(
                    comparison = workoutRepository.getMonthComparison(year, month),
                    metricsSummary = workoutRepository.getMetricsSummaryForMonth(year, month),
                    goalMet = workoutRepository.getMonthlyGoalMetComparison(year, month, settings)
                )
            }.onSuccess { trends ->
                showMonthComparison(trends.comparison, trends.metricsSummary)
                showGoalMetTrend(trends.goalMet)
            }.onFailure { throwable ->
                android.util.Log.e(TAG, "Error loading month comparison", throwable)
                binding.monthlyTrendHeader.visibility = View.GONE
                binding.monthComparisonContainer.visibility = View.GONE
                binding.monthMetricsCard.visibility = View.GONE
                binding.monthGoalMetCard.visibility = View.GONE
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private data class MonthTrends(
        val comparison: MonthComparison,
        val metricsSummary: WorkoutMetricsSummary?,
        val goalMet: MonthlyGoalMetComparison
    )

    fun loadWorkoutTypeDistribution(year: Int, month: Int) {
        activity.lifecycleScope.launch {
            runCatching {
                workoutRepository.getWorkoutTypeDistribution(year, month)
            }.onSuccess { distribution ->
                if (distribution.isNotEmpty()) {
                    displayWorkoutTypeDistribution(distribution)
                    binding.workoutTypesHeader.visibility = View.VISIBLE
                    binding.workoutTypesContainer.visibility = View.VISIBLE
                } else {
                    binding.workoutTypesHeader.visibility = View.GONE
                    binding.workoutTypesContainer.visibility = View.GONE
                }
                binding.swipeRefreshLayout.isRefreshing = false
            }.onFailure { throwable ->
                android.util.Log.e(TAG, "Error loading workout type distribution", throwable)
                binding.workoutTypesHeader.visibility = View.GONE
                binding.workoutTypesContainer.visibility = View.GONE
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    fun applyEmptyUi() {
        binding.monthComparisonWorkoutsValue.text = activity.getString(R.string.placeholder_zero)
        binding.monthComparisonMinutesValue.text = formatMinutes(0)
        binding.monthComparisonWorkoutsBadgeContainer.visibility = View.GONE
        binding.monthComparisonWorkoutsEmptyState.visibility = View.VISIBLE
        binding.monthComparisonMinutesBadgeContainer.visibility = View.GONE
        binding.monthComparisonMinutesEmptyState.visibility = View.VISIBLE
        binding.monthMetricsCard.visibility = View.GONE
        binding.monthGoalMetCard.visibility = View.GONE
    }

    private fun showGoalMetTrend(comparison: MonthlyGoalMetComparison) {
        val current = comparison.current
        if (!current.active) {
            binding.monthGoalMetCard.visibility = View.GONE
            return
        }
        binding.monthGoalMetCard.visibility = View.VISIBLE

        if (current.totalWeeks <= 0) {
            binding.monthGoalMetValue.visibility = View.GONE
            binding.monthGoalMetBadgeContainer.visibility = View.GONE
            binding.monthGoalMetEmptyState.visibility = View.VISIBLE
            return
        }

        binding.monthGoalMetEmptyState.visibility = View.GONE
        binding.monthGoalMetValue.visibility = View.VISIBLE
        binding.monthGoalMetValue.text = if (current.totalWeeks == 1) {
            activity.getString(R.string.format_weekly_goal_met_weeks_singular, current.weeksMet)
        } else {
            activity.getString(
                R.string.format_weekly_goal_met_weeks,
                current.weeksMet,
                current.totalWeeks
            )
        }

        val previous = comparison.previous
        if (previous.active && previous.totalWeeks > 0) {
            updateComparisonBadge(
                binding.monthGoalMetArrow,
                binding.monthGoalMetChange,
                goalMetChangePercent(current.weeksMet, previous.weeksMet),
                current.weeksMet,
                previous.weeksMet
            )
            binding.monthGoalMetBadgeContainer.visibility = View.VISIBLE
        } else {
            binding.monthGoalMetBadgeContainer.visibility = View.GONE
        }
    }

    private fun goalMetChangePercent(current: Int, previous: Int): Double {
        return when {
            previous > 0 -> ((current - previous).toDouble() / previous) * PERCENT_MAX
            current > 0 -> PERCENT_MAX.toDouble()
            else -> 0.0
        }
    }

    private fun showMonthComparison(comparison: MonthComparison, metricsSummary: WorkoutMetricsSummary?) {
        binding.monthlyTrendHeader.visibility = View.VISIBLE
        binding.monthComparisonContainer.visibility = View.VISIBLE
        showWorkoutComparison(comparison)
        showMinutesComparison(comparison)
        showMetricsSummary(comparison, metricsSummary)
        binding.swipeRefreshLayout.isRefreshing = false
    }

    private fun showWorkoutComparison(comparison: MonthComparison) {
        if (comparison.previousMonthWorkouts > 0 || comparison.currentMonthWorkouts > 0) {
            binding.monthComparisonWorkoutsValue.text = if (comparison.currentMonthWorkouts == 1) {
                activity.getString(R.string.format_workouts_count_singular)
            } else {
                activity.getString(R.string.format_workouts_count, comparison.currentMonthWorkouts)
            }
            updateComparisonBadge(
                binding.monthComparisonWorkoutsArrow,
                binding.monthComparisonWorkoutsChange,
                comparison.workoutChangePercent,
                comparison.currentMonthWorkouts,
                comparison.previousMonthWorkouts
            )
            binding.monthComparisonWorkoutsBadgeContainer.visibility = View.VISIBLE
            binding.monthComparisonWorkoutsEmptyState.visibility = View.GONE
        } else {
            binding.monthComparisonWorkoutsValue.text = activity.getString(R.string.placeholder_zero)
            binding.monthComparisonWorkoutsBadgeContainer.visibility = View.GONE
            binding.monthComparisonWorkoutsEmptyState.visibility = View.VISIBLE
        }
    }

    private fun showMinutesComparison(comparison: MonthComparison) {
        if (comparison.previousMonthMinutes > 0 || comparison.currentMonthMinutes > 0) {
            binding.monthComparisonMinutesValue.text = activity.getString(
                R.string.format_minutes_value,
                formatMinutes(comparison.currentMonthMinutes)
            )
            updateComparisonBadge(
                binding.monthComparisonMinutesArrow,
                binding.monthComparisonMinutesChange,
                comparison.minutesChangePercent,
                comparison.currentMonthMinutes,
                comparison.previousMonthMinutes
            )
            binding.monthComparisonMinutesBadgeContainer.visibility = View.VISIBLE
            binding.monthComparisonMinutesEmptyState.visibility = View.GONE
        } else {
            binding.monthComparisonMinutesValue.text = formatMinutes(0)
            binding.monthComparisonMinutesBadgeContainer.visibility = View.GONE
            binding.monthComparisonMinutesEmptyState.visibility = View.VISIBLE
        }
    }

    private fun showMetricsSummary(comparison: MonthComparison, metricsSummary: WorkoutMetricsSummary?) {
        val stepCount = metricsSummary?.stepCount
        val heartRate = metricsSummary?.averageHeartRateBpm
        val showPlaceholders = metricsEnabledProvider() && comparison.currentMonthWorkouts > 0
        val cardFormatter = WorkoutMetricsCardFormatter(activity)
        val stepDisplay = cardFormatter.stepsCardValue(stepCount, showPlaceholders)
        val heartRateDisplay = cardFormatter.averageHeartRateCardValue(heartRate, showPlaceholders)
        binding.monthMetricsStepsCard.visibility = if (stepDisplay != null) View.VISIBLE else View.GONE
        binding.monthMetricsHeartRateCard.visibility = if (heartRateDisplay != null) View.VISIBLE else View.GONE
        stepDisplay?.let { bindMetricValue(binding.monthMetricsStepsValue, it) }
        heartRateDisplay?.let { bindMetricValue(binding.monthMetricsHeartRateValue, it) }
        binding.monthMetricsCard.visibility =
            if (stepDisplay != null || heartRateDisplay != null) View.VISIBLE else View.GONE
    }

    private fun bindMetricValue(
        valueView: TextView,
        display: WorkoutPhaseHeartRateDisplay
    ) {
        valueView.text = display.text
        val colorRes = if (display.isPlaceholder) R.color.text_secondary else R.color.text_primary
        valueView.setTextColor(ContextCompat.getColor(activity, colorRes))
        val style = if (display.isPlaceholder) {
            android.graphics.Typeface.NORMAL
        } else {
            android.graphics.Typeface.BOLD
        }
        valueView.setTypeface(valueView.typeface, style)
    }

    private fun updateComparisonBadge(
        arrowView: TextView,
        percentView: TextView,
        changePercent: Double,
        currentValue: Int,
        previousValue: Int
    ) {
        val increaseColor = accentColorProvider()
        val decreaseColor = ContextCompat.getColor(activity, android.R.color.holo_red_dark)
        val neutralColor = ContextCompat.getColor(activity, R.color.text_secondary)

        when {
            changePercent > 0 -> showPercentBadge(arrowView, percentView, "▲", changePercent, increaseColor)
            changePercent < 0 -> showPercentBadge(arrowView, percentView, "▼", changePercent, decreaseColor)
            previousValue == 0 && currentValue > 0 -> {
                arrowView.text = activity.getString(R.string.label_new)
                arrowView.setTextColor(increaseColor)
                arrowView.visibility = View.VISIBLE
                percentView.visibility = View.GONE
            }
            changePercent == 0.0 && previousValue > 0 -> {
                arrowView.text = "\u2212"
                arrowView.setTextColor(neutralColor)
                arrowView.visibility = View.VISIBLE
                percentView.text = activity.getString(R.string.format_percent_change, 0)
                percentView.setTextColor(neutralColor)
                percentView.visibility = View.VISIBLE
            }
            else -> {
                arrowView.visibility = View.GONE
                percentView.visibility = View.GONE
            }
        }
    }

    private fun showPercentBadge(
        arrowView: TextView,
        percentView: TextView,
        symbol: String,
        changePercent: Double,
        color: Int
    ) {
        arrowView.text = symbol
        arrowView.setTextColor(color)
        arrowView.visibility = View.VISIBLE
        percentView.text = activity.getString(R.string.format_percent_change, abs(changePercent.toInt()))
        percentView.setTextColor(color)
        percentView.visibility = View.VISIBLE
    }

    private fun displayWorkoutTypeDistribution(distribution: Map<String, Int>) {
        binding.workoutTypesContainer.removeAllViews()
        val total = distribution.values.sum()
        val sorted = distribution.toList().sortedByDescending { it.second }

        sorted.forEach { (type, count) ->
            val itemView = LayoutInflater.from(activity).inflate(
                R.layout.item_workout_type,
                binding.workoutTypesContainer,
                false
            )
            val percentage = ((count.toFloat() / total.toFloat()) * PERCENT_MAX).toInt()
            itemView.findViewById<TextView>(R.id.workoutTypeName).text = type.replace("1 Rounds", "1 Round")
            itemView.findViewById<TextView>(R.id.workoutTypeCount).text =
                activity.getString(R.string.format_workout_type_count, count, percentage)
            itemView.findViewById<android.widget.ProgressBar>(R.id.workoutTypeProgress).apply {
                progress = percentage
                progressTintList = android.content.res.ColorStateList.valueOf(accentColorProvider())
            }
            binding.workoutTypesContainer.addView(itemView)
        }
    }

    companion object {
        private const val TAG = "StatsTrends"
        private const val PERCENT_MAX = 100
    }
}
