package com.oceanofmaya.intervalwalktrainer

import android.content.Context
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ImageSpan
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

class WorkoutMetricsUiFormatter(private val context: Context) {
    private val integerFormat = NumberFormat.getIntegerInstance(Locale.getDefault())

    fun savedMetricsText(session: WorkoutSession): String? {
        return savedMetricItems(session).toPlainText()
    }

    fun savedMetricItems(session: WorkoutSession): List<WorkoutMetricDisplayItem> = buildList {
        session.stepCount?.let { add(stepsItem(formatSteps(it))) }
        session.averageHeartRateBpm?.let { add(heartItem(formatAverageHeartRate(it))) }
        phaseMetricsText(session)?.let { add(heartItem(it)) }
    }

    fun detailedMetricsRichText(
        session: WorkoutSession,
        showMetricPlaceholders: Boolean = false
    ): CharSequence? {
        return detailedMetricItems(session, showMetricPlaceholders).toRichText()
    }

    fun detailedMetricItems(
        session: WorkoutSession,
        showMetricPlaceholders: Boolean = false
    ): List<WorkoutMetricDisplayItem> = buildList {
        when {
            session.stepCount != null -> add(stepsItem(formatSteps(session.stepCount)))
            showMetricPlaceholders -> add(stepsItem(unavailableLabel()))
        }
        heartMetricsText(session, showMetricPlaceholders)?.let { add(heartItem(it)) }
        phaseMetricsText(session)?.let { add(heartItem(it)) }
    }

    fun insightMetricItems(session: WorkoutSession): List<WorkoutMetricDisplayItem> = buildList {
        session.stepCount?.let { add(stepsItem(formatSteps(it))) }
        session.averageHeartRateBpm?.let { add(heartItem(formatAverageHeartRate(it))) }
    }

    fun insightMetricsRichText(session: WorkoutSession): CharSequence? {
        return insightMetricItems(session).toRichText()
    }

    fun insightMetricsPlainText(session: WorkoutSession): String? {
        return insightMetricItems(session).toPlainText()
    }

    fun phaseMetricsText(summary: WorkoutMetricsSummary): String? {
        val showPhaseMetrics = summary.formulaSnapshot != null ||
            summary.phaseWindows.isNotEmpty() ||
            summary.fastPhaseAverageHeartRateBpm != null ||
            summary.slowPhaseAverageHeartRateBpm != null
        if (!showPhaseMetrics) return null
        return pairedPhaseMetricsText(
            fastAverage = summary.fastPhaseAverageHeartRateBpm,
            slowAverage = summary.slowPhaseAverageHeartRateBpm,
            hasWorkoutHeartRate = summary.averageHeartRateBpm != null
        )
    }

    fun phaseMetricsText(session: WorkoutSession): String? {
        if (!WorkoutPhaseMetricsDisplay.shouldShowPhaseMetrics(session)) return null
        return pairedPhaseMetricsText(
            fastAverage = session.fastPhaseAverageHeartRateBpm,
            slowAverage = session.slowPhaseAverageHeartRateBpm,
            hasWorkoutHeartRate = session.averageHeartRateBpm != null
        )
    }

    private fun pairedPhaseMetricsText(
        fastAverage: Int?,
        slowAverage: Int?,
        hasWorkoutHeartRate: Boolean
    ): String {
        val cardFormatter = WorkoutMetricsCardFormatter(context)
        val fast = cardFormatter.phaseHeartRateCardValue(
            heartRateBpm = fastAverage,
            hasWorkoutHeartRate = hasWorkoutHeartRate,
            fastAverage = fastAverage,
            slowAverage = slowAverage
        )
        val slow = cardFormatter.phaseHeartRateCardValue(
            heartRateBpm = slowAverage,
            hasWorkoutHeartRate = hasWorkoutHeartRate,
            fastAverage = fastAverage,
            slowAverage = slowAverage
        )
        return listOf(
            context.getString(R.string.format_fast_phase_heart_rate, fast.text),
            context.getString(R.string.format_slow_phase_heart_rate, slow.text)
        ).joinToString(context.getString(R.string.separator_bullet))
    }

    private fun heartMetricsText(
        session: WorkoutSession,
        showMetricPlaceholders: Boolean = false
    ): String? {
        val min = session.minHeartRateBpm
        val max = session.maxHeartRateBpm
        val parts = buildList {
            when {
                session.averageHeartRateBpm != null -> add(formatAverageHeartRate(session.averageHeartRateBpm))
                showMetricPlaceholders -> add(
                    context.getString(R.string.format_average_heart_rate_placeholder, unavailableLabel())
                )
            }
            when {
                min != null && max != null -> add(
                    context.getString(R.string.format_heart_rate_range, min, max)
                )
                showMetricPlaceholders -> add(
                    context.getString(
                        R.string.format_heart_rate_range_placeholder,
                        unavailableLabel()
                    )
                )
            }
        }
        return parts.joinToString(context.getString(R.string.separator_bullet))
            .takeIf { it.isNotBlank() }
    }

    private fun unavailableLabel(): String {
        return context.getString(R.string.label_metrics_unavailable)
    }

    private fun formatSteps(stepCount: Int): String {
        return context.getString(R.string.format_steps, integerFormat.format(stepCount))
    }

    private fun formatAverageHeartRate(heartRateBpm: Int): String {
        return context.getString(R.string.format_average_heart_rate, heartRateBpm)
    }

    private fun stepsItem(text: String): WorkoutMetricDisplayItem {
        return WorkoutMetricDisplayItem(R.drawable.outline_steps_24, text)
    }

    private fun heartItem(text: String): WorkoutMetricDisplayItem {
        return WorkoutMetricDisplayItem(R.drawable.outline_ecg_heart_24, text)
    }

    private fun List<WorkoutMetricDisplayItem>.toPlainText(): String? {
        return joinToString(context.getString(R.string.separator_bullet)) { it.text }
            .takeIf { it.isNotBlank() }
    }

    private fun List<WorkoutMetricDisplayItem>.toRichText(): CharSequence? {
        if (isEmpty()) return null
        val builder = SpannableStringBuilder()
        forEachIndexed { index, item ->
            if (index > 0) builder.append('\n')
            builder.appendIcon(item.iconResId)
            builder.append('\u00A0')
            builder.append(item.text)
        }
        return builder
    }

    private fun SpannableStringBuilder.appendIcon(@DrawableRes iconResId: Int) {
        val density = runCatching { context.resources.displayMetrics.density }.getOrNull() ?: return
        val drawable = runCatching { ContextCompat.getDrawable(context, iconResId)?.mutate() }.getOrNull()
            ?: return

        val iconSize = (ICON_SIZE_DP * density).roundToInt()
        drawable.setBounds(0, 0, iconSize, iconSize)
        drawable.setTint(ContextCompat.getColor(context, R.color.text_secondary))

        val start = length
        append(ICON_PLACEHOLDER)
        setSpan(
            ImageSpan(drawable, ImageSpan.ALIGN_BOTTOM),
            start,
            start + ICON_PLACEHOLDER.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
    }

    data class WorkoutMetricDisplayItem(
        @param:DrawableRes val iconResId: Int,
        val text: String
    )

    private companion object {
        const val ICON_SIZE_DP = 16
        const val ICON_PLACEHOLDER = " "
    }
}
