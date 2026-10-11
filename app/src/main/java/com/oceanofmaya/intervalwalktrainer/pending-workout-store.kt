@file:Suppress("MatchingDeclarationName")

package com.oceanofmaya.intervalwalktrainer

import android.content.Context

/**
 * A small synchronous recovery marker written at actual timer completion before Room work starts.
 * Room's unique completion ID makes replay after process death safe.
 */
class PendingWorkoutStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    fun save(workout: PendingWorkout): Boolean {
        return preferences.edit()
            .putString(KEY_ID, workout.completionId)
            .putString(KEY_TYPE, workout.workoutType)
            .putInt(KEY_MINUTES, workout.minutes)
            .putLong(KEY_COMPLETED_AT, workout.completedAtMillis)
            .commit()
    }

    fun load(): PendingWorkout? {
        val id = preferences.getString(KEY_ID, null).orEmpty()
        val type = preferences.getString(KEY_TYPE, null).orEmpty()
        val minutes = preferences.getInt(KEY_MINUTES, 0)
        val completedAt = preferences.getLong(KEY_COMPLETED_AT, 0L)
        return PendingWorkout(id, type, minutes, completedAt).takeIf { it.isValid }
    }

    fun clearIfMatches(completionId: String) {
        if (preferences.getString(KEY_ID, null) == completionId) {
            preferences.edit().clear().apply()
        }
    }

    data class PendingWorkout(
        val completionId: String,
        val workoutType: String,
        val minutes: Int,
        val completedAtMillis: Long
    ) {
        val isValid: Boolean
            get() = completionId.isNotBlank() &&
                workoutType.isNotBlank() &&
                minutes > 0 &&
                completedAtMillis > 0L
    }

    private companion object {
        const val PREFERENCES_NAME = "pending_completed_workout"
        const val KEY_ID = "completion-id"
        const val KEY_TYPE = "workout-type"
        const val KEY_MINUTES = "minutes"
        const val KEY_COMPLETED_AT = "completed-at"
    }
}
