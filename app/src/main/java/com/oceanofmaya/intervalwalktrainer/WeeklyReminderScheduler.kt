package com.oceanofmaya.intervalwalktrainer

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

class WeeklyReminderScheduler(private val context: Context) {
    private val appContext = context.applicationContext
    private val sharedPreferences = appContext.getSharedPreferences(
        WeeklyGoalPreferences.PREFS_NAME,
        Context.MODE_PRIVATE
    )

    suspend fun scheduleNextReminder(nowMillis: Long = System.currentTimeMillis()) {
        val reminderSettings = WeeklyGoalPreferences.loadReminderSettings(sharedPreferences)
        val goalSettings = WeeklyGoalPreferences.loadGoalSettings(sharedPreferences)
        val shouldPauseForMetGoal = reminderSettings.pauseWhenGoalMet &&
            goalSettings.enabled &&
            isGoalMet(goalSettings, nowMillis)
        val triggerAtMillis = if (goalSettings.enabled && !shouldPauseForMetGoal) {
            WeeklyGoalCalculator.nextReminderTimeMillis(reminderSettings, nowMillis)
        } else {
            null
        }
        if (!reminderSettings.enabled || triggerAtMillis == null) {
            cancelReminder()
        } else {
            val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            reminderPendingIntent(PendingIntent.FLAG_UPDATE_CURRENT)?.let { pendingIntent ->
                if (canScheduleExactReminders(alarmManager)) {
                    scheduleExactReminder(alarmManager, triggerAtMillis, pendingIntent)
                } else {
                    cancelReminder()
                }
            }
        }
    }

    fun canScheduleExactReminders(): Boolean {
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return canScheduleExactReminders(alarmManager)
    }

    fun cancelReminder() {
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(reminderPendingIntent(PendingIntent.FLAG_NO_CREATE) ?: return)
    }

    suspend fun showReminderIfAllowed(nowMillis: Long = System.currentTimeMillis()) {
        val reminderSettings = WeeklyGoalPreferences.loadReminderSettings(sharedPreferences)
        val goalSettings = WeeklyGoalPreferences.loadGoalSettings(sharedPreferences)
        val shouldPauseForMetGoal = reminderSettings.pauseWhenGoalMet &&
            goalSettings.enabled &&
            isGoalMet(goalSettings, nowMillis)
        val canShowReminder = reminderSettings.enabled &&
            goalSettings.enabled &&
            !shouldPauseForMetGoal &&
            canPostNotifications()
        if (canShowReminder) {
            createReminderChannel()
            postReminderNotification()
        }
    }

    private suspend fun isGoalMet(settings: WeeklyGoalSettings, nowMillis: Long): Boolean {
        val database = AppDatabase.getDatabase(appContext)
        val repository = WorkoutRepository(database.workoutDao(), database.workoutSessionDao(), database)
        return repository.getWeeklyGoalProgress(settings, nowMillis).isGoalMet
    }

    private fun canPostNotifications(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun canScheduleExactReminders(alarmManager: AlarmManager): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
    }

    @SuppressLint("ScheduleExactAlarm")
    private fun scheduleExactReminder(
        alarmManager: AlarmManager,
        triggerAtMillis: Long,
        pendingIntent: PendingIntent
    ) {
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
    }

    private fun createReminderChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val notificationManager = appContext.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            REMINDER_CHANNEL_ID,
            appContext.getString(R.string.notif_weekly_reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = appContext.getString(R.string.notif_weekly_reminder_channel_description)
        }
        notificationManager.createNotificationChannel(channel)
    }

    @SuppressLint("MissingPermission")
    private fun postReminderNotification() {
        val notificationManager = NotificationManagerCompat.from(appContext)
        notificationManager.cancel(REMINDER_NOTIFICATION_ID)
        notificationManager.notify(
            REMINDER_NOTIFICATION_ID,
            NotificationCompat.Builder(appContext, REMINDER_CHANNEL_ID)
                .setSmallIcon(R.drawable.baseline_notifications_24)
                .setContentTitle(appContext.getString(R.string.notif_weekly_reminder_title))
                .setContentText(appContext.getString(R.string.notif_weekly_reminder_body))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(mainActivityPendingIntent())
                .build()
        )
    }

    private fun reminderPendingIntent(flags: Int): PendingIntent? {
        val intent = Intent(appContext, WeeklyGoalReminderReceiver::class.java)
        return PendingIntent.getBroadcast(
            appContext,
            REMINDER_REQUEST_CODE,
            intent,
            flags or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun mainActivityPendingIntent(): PendingIntent {
        val intent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            appContext,
            REMINDER_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        private const val REMINDER_REQUEST_CODE = 1300
        private const val REMINDER_NOTIFICATION_ID = 1301
        private const val REMINDER_CHANNEL_ID = "weekly_goal_reminders"
    }
}
