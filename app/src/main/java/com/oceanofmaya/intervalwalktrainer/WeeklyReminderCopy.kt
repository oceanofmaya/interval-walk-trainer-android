package com.oceanofmaya.intervalwalktrainer

import androidx.annotation.StringRes

object WeeklyReminderCopy {
    @StringRes
    fun titleResId(hasCompletedWalkToday: Boolean): Int {
        return if (hasCompletedWalkToday) {
            R.string.notif_weekly_reminder_title_already_walked
        } else {
            R.string.notif_weekly_reminder_title
        }
    }

    @StringRes
    fun bodyResId(hasCompletedWalkToday: Boolean): Int {
        return if (hasCompletedWalkToday) {
            R.string.notif_weekly_reminder_body_already_walked
        } else {
            R.string.notif_weekly_reminder_body
        }
    }
}
