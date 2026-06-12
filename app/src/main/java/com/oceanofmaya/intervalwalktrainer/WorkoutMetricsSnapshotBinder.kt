package com.oceanofmaya.intervalwalktrainer

import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat

class WorkoutMetricsSnapshotBinder(
    private val root: View
) {
    private val titleView = root.findViewById<TextView>(R.id.workoutMetricsSnapshotTitle)
    private val statusView = root.findViewById<TextView>(R.id.workoutMetricsSnapshotStatus)

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
        statusView.text = if (metrics == null) {
            root.context.getString(R.string.body_workout_metrics_snapshot_unavailable)
        } else {
            root.context.getString(R.string.body_workout_metrics_snapshot)
        }
    }

    private fun shouldShow(metricsEnabled: Boolean, saveWorkoutsEnabled: Boolean): Boolean {
        return metricsEnabled && saveWorkoutsEnabled
    }
}
