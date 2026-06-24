package com.oceanofmaya.intervalwalktrainer

import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat

class WorkoutMetricsSnapshotBinder(
    private val root: View
) {
    private val titleView = root.findViewById<TextView>(R.id.workoutMetricsSnapshotTitle)
    private val statusView = root.findViewById<TextView>(R.id.workoutMetricsSnapshotStatus)
    private val valuesView = root.findViewById<View>(R.id.workoutMetricsSnapshotValues)
    private val stepsView = root.findViewById<TextView>(R.id.workoutMetricsSnapshotSteps)
    private val heartRateView = root.findViewById<TextView>(R.id.workoutMetricsSnapshotHeartRate)
    private val phasesView = root.findViewById<TextView>(R.id.workoutMetricsSnapshotPhases)

    fun hide() {
        root.visibility = View.GONE
    }

    fun showLoading(metricsEnabled: Boolean, saveWorkoutsEnabled: Boolean) {
        if (!shouldShow(metricsEnabled, saveWorkoutsEnabled)) {
            hide()
            return
        }
        root.visibility = View.VISIBLE
        titleView.setTextColor(ContextCompat.getColor(root.context, R.color.text_primary))
        statusView.text = root.context.getString(R.string.body_workout_metrics_snapshot_loading)
        valuesView.visibility = View.GONE
    }

    fun bind(
        metricsEnabled: Boolean,
        saveWorkoutsEnabled: Boolean,
        metrics: WorkoutMetricsSummary?
    ) {
        if (!shouldShow(metricsEnabled, saveWorkoutsEnabled)) {
            hide()
            return
        }

        root.visibility = View.VISIBLE
        titleView.setTextColor(ContextCompat.getColor(root.context, R.color.text_primary))
        val formatter = WorkoutMetricsUiFormatter(root.context)
        bindValueRows(formatter, metrics)
        statusView.text = when {
            metrics == null -> root.context.getString(R.string.body_workout_metrics_snapshot_unavailable)
            metrics.hasDisplayableValue -> root.context.getString(R.string.body_workout_metrics_snapshot)
            else -> root.context.getString(R.string.body_workout_metrics_snapshot_pending)
        }
    }

    private fun bindValueRows(
        formatter: WorkoutMetricsUiFormatter,
        metrics: WorkoutMetricsSummary?
    ) {
        val items = metrics?.let { formatter.summaryMetricItems(it) }.orEmpty()
        val steps = items.firstOrNull { it.iconResId == R.drawable.outline_steps_24 }?.text
        val heartItems = items.filter { it.iconResId == R.drawable.outline_ecg_heart_24 }
        valuesView.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
        bindRow(stepsView, steps)
        bindRow(heartRateView, heartItems.getOrNull(0)?.text)
        bindRow(phasesView, heartItems.getOrNull(1)?.text)
    }

    private fun bindRow(view: TextView, text: String?) {
        view.visibility = if (text.isNullOrBlank()) View.GONE else View.VISIBLE
        view.text = text.orEmpty()
    }

    private fun shouldShow(metricsEnabled: Boolean, saveWorkoutsEnabled: Boolean): Boolean {
        return metricsEnabled && saveWorkoutsEnabled
    }
}
