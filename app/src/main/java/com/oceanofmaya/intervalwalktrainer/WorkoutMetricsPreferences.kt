package com.oceanofmaya.intervalwalktrainer

import android.content.SharedPreferences

object WorkoutMetricsPreferences {
    const val KEY_WORKOUT_METRICS_ENABLED = "workout_metrics_enabled"

    fun isEnabled(preferences: SharedPreferences): Boolean {
        return preferences.getBoolean(KEY_WORKOUT_METRICS_ENABLED, false)
    }
}
