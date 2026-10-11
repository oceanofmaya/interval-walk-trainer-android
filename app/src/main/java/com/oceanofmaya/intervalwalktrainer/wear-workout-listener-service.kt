@file:Suppress("MatchingDeclarationName")

package com.oceanofmaya.intervalwalktrainer

import android.net.Uri
import android.util.Log
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.launch

/**
 * Receives durable Wear OS completion data items and records them exactly once on the phone.
 */
class WearWorkoutListenerService : WearableListenerService() {
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents
            .filter { event ->
                event.type == DataEvent.TYPE_CHANGED &&
                    event.dataItem.uri.path
                        ?.startsWith(WearSyncContract.COMPLETED_WORKOUT_PATH_PREFIX) == true
            }
            .forEach { event ->
                val map = DataMapItem.fromDataItem(event.dataItem).dataMap
                val completion = WearCompletion(
                    id = map.getString(WearSyncContract.KEY_COMPLETION_ID).orEmpty(),
                    completedAtMillis = map.getLong(WearSyncContract.KEY_COMPLETED_AT_MILLIS),
                    workoutType = map.getString(WearSyncContract.KEY_WORKOUT_TYPE).orEmpty(),
                    minutes = map.getInt(WearSyncContract.KEY_MINUTES),
                    uri = event.dataItem.uri
                )
                if (completion.isValid) {
                    recordCompletion(completion)
                }
            }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun recordCompletion(completion: WearCompletion) {
        val app = application as IntervalWalkTrainerApp
        app.applicationScope.launch {
            try {
                val saveEnabled = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .getBoolean(KEY_SAVE_WORKOUTS, true)
                if (saveEnabled) {
                    val database = AppDatabase.getDatabase(applicationContext)
                    WorkoutRepository(
                        database.workoutDao(),
                        database.workoutSessionDao(),
                        database
                    ).recordWorkout(
                        minutes = completion.minutes,
                        workoutType = completion.workoutType,
                        completionId = completion.id,
                        completedAtMillis = completion.completedAtMillis
                    )
                }
                Wearable.getDataClient(applicationContext).deleteDataItems(completion.uri)
                WeeklyReminderScheduler(applicationContext).scheduleNextReminder()
            } catch (error: Exception) {
                // Keep the DataItem in place so Google Play services can redeliver it.
                Log.e(TAG, "Unable to record Wear OS workout ${completion.id}", error)
            }
        }
    }

    private data class WearCompletion(
        val id: String,
        val completedAtMillis: Long,
        val workoutType: String,
        val minutes: Int,
        val uri: Uri
    ) {
        val isValid: Boolean
            get() = id.isNotBlank() &&
                completedAtMillis > 0L &&
                workoutType.isNotBlank() &&
                minutes > 0
    }

    private companion object {
        const val TAG = "WearWorkoutListener"
        const val PREFS_NAME = "interval_walk_trainer_prefs"
        const val KEY_SAVE_WORKOUTS = "save_workouts"
    }
}
