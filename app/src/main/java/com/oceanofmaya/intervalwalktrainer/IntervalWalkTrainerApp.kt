package com.oceanofmaya.intervalwalktrainer

import android.app.Application
import android.util.Log
import com.google.android.material.color.DynamicColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class IntervalWalkTrainerApp : Application() {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        DynamicColors.applyToActivitiesIfAvailable(this)
        recoverPendingWorkout()
    }

    @Suppress("TooGenericExceptionCaught")
    private fun recoverPendingWorkout() {
        val pendingStore = PendingWorkoutStore(this)
        val pending = pendingStore.load() ?: return
        applicationScope.launch {
            try {
                val database = AppDatabase.getDatabase(this@IntervalWalkTrainerApp)
                WorkoutRepository(
                    database.workoutDao(),
                    database.workoutSessionDao(),
                    database
                ).recordWorkout(
                    minutes = pending.minutes,
                    workoutType = pending.workoutType,
                    completionId = pending.completionId,
                    completedAtMillis = pending.completedAtMillis
                )
                pendingStore.clearIfMatches(pending.completionId)
                WeeklyReminderScheduler(this@IntervalWalkTrainerApp).scheduleNextReminder()
            } catch (error: Exception) {
                Log.e("IntervalWalkTrainerApp", "Pending workout recovery failed", error)
            }
        }
    }
}
