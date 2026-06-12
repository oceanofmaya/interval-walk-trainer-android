package com.oceanofmaya.intervalwalktrainer

import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.oceanofmaya.intervalwalktrainer.databinding.ActivityStatsBinding
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.launch
import kotlin.math.abs

class StatsTrendsController(
    private val activity: AppCompatActivity,
    private val binding: ActivityStatsBinding,
    private val workoutRepository: WorkoutRepository,
    private val accentColorProvider: () -> Int,
    private val formatMinutes: (Int) -> String
) {
    fun loadMonthComparison(year: Int, month: Int) {
        activity.lifecycleScope.launch {
            runCatching {
                workoutRepository.getMonthComparison(year, month) to
                    workoutRepository.getMetricsSummaryForMonth(year, month)
            }.onSuccess { (comparison, metricsSummary) ->
                showMonthComparison(comparison, metricsSummary)
            }.onFailure { throwable ->
                android.util.Log.e(TAG, "Error loading month comparison", throwable)
                binding.monthlyTrendHeader.visibility = View.GONE
                binding.monthComparisonContainer.visibility = View.GONE
                binding.monthMetricsCard.visibility = View.GONE
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }

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
    }

    private fun showMonthComparison(comparison: MonthComparison, metricsSummary: WorkoutMetricsSummary?) {
        binding.monthlyTrendHeader.visibility = View.VISIBLE
        binding.monthComparisonContainer.visibility = View.VISIBLE
        showWorkoutComparison(comparison)
        showMinutesComparison(comparison)
        showMetricsSummary(metricsSummary)
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

    private fun showMetricsSummary(metricsSummary: WorkoutMetricsSummary?) {
        val stepCount = metricsSummary?.stepCount
        val heartRate = metricsSummary?.averageHeartRateBpm
        binding.monthMetricsStepsCard.visibility = if (stepCount != null) View.VISIBLE else View.GONE
        binding.monthMetricsHeartRateCard.visibility = if (heartRate != null) View.VISIBLE else View.GONE
        stepCount?.let {
            binding.monthMetricsStepsValue.text = integerFormat.format(it)
        }
        heartRate?.let {
            binding.monthMetricsHeartRateValue.text = activity.getString(R.string.format_heart_rate_bpm, it)
        }
        binding.monthMetricsCard.visibility = if (stepCount != null || heartRate != null) {
            View.VISIBLE
        } else {
            View.GONE
        }
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
        private val integerFormat = NumberFormat.getIntegerInstance(Locale.getDefault())
    }
}
