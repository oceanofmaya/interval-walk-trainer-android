package com.oceanofmaya.intervalwalktrainer

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WeeklyGoalReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val scheduler = WeeklyReminderScheduler(context)
                scheduler.showReminderIfAllowed()
                scheduler.scheduleNextReminder()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
