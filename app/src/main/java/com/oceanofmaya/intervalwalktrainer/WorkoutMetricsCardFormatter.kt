package com.oceanofmaya.intervalwalktrainer

import android.content.Context
import java.text.NumberFormat
import java.util.Locale

class WorkoutMetricsCardFormatter(private val context: Context) {
    private val integerFormat = NumberFormat.getIntegerInstance(Locale.getDefault())

    fun stepsCardValue(stepCount: Int?, showMetricPlaceholders: Boolean): WorkoutPhaseHeartRateDisplay? {
        return when {
            stepCount != null -> WorkoutPhaseHeartRateDisplay(
                text = integerFormat.format(stepCount),
                isPlaceholder = false
            )
            showMetricPlaceholders -> placeholderDisplay()
            else -> null
        }
    }

    fun averageHeartRateCardValue(
        heartRateBpm: Int?,
        showMetricPlaceholders: Boolean
    ): WorkoutPhaseHeartRateDisplay? {
        return when {
            heartRateBpm != null -> WorkoutPhaseHeartRateDisplay(
                text = context.getString(R.string.format_heart_rate_bpm, heartRateBpm),
                isPlaceholder = false
            )
            showMetricPlaceholders -> placeholderDisplay()
            else -> null
        }
    }

    fun phaseHeartRateCardValue(
        heartRateBpm: Int?,
        hasWorkoutHeartRate: Boolean,
        fastAverage: Int? = null,
        slowAverage: Int? = null
    ): WorkoutPhaseHeartRateDisplay {
        return WorkoutPhaseMetricsDisplay.formatPhaseHeartRate(
            heartRateBpm = heartRateBpm,
            hasWorkoutHeartRate = hasWorkoutHeartRate,
            anyPhaseHeartRateResolved = fastAverage != null || slowAverage != null,
            unavailableLabel = unavailableLabel(),
            pendingLabel = context.getString(R.string.label_metrics_pending_sync),
            formatBpm = { bpm -> context.getString(R.string.format_heart_rate_bpm, bpm) }
        )
    }

    private fun unavailableLabel(): String {
        return context.getString(R.string.label_metrics_unavailable)
    }

    private fun placeholderDisplay(): WorkoutPhaseHeartRateDisplay {
        return WorkoutPhaseHeartRateDisplay(
            text = unavailableLabel(),
            isPlaceholder = true
        )
    }
}
