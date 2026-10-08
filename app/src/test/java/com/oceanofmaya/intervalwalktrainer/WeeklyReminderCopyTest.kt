package com.oceanofmaya.intervalwalktrainer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WeeklyReminderCopyTest {
    @Test
    fun `reminder asks for a walk when none is completed today`() {
        assertEquals(
            R.string.notif_weekly_reminder_title,
            WeeklyReminderCopy.titleResId(hasCompletedWalkToday = false)
        )
        assertEquals(
            R.string.notif_weekly_reminder_body,
            WeeklyReminderCopy.bodyResId(hasCompletedWalkToday = false)
        )
    }

    @Test
    fun `reminder acknowledges a walk already completed today`() {
        assertEquals(
            R.string.notif_weekly_reminder_title_already_walked,
            WeeklyReminderCopy.titleResId(hasCompletedWalkToday = true)
        )
        assertEquals(
            R.string.notif_weekly_reminder_body_already_walked,
            WeeklyReminderCopy.bodyResId(hasCompletedWalkToday = true)
        )
    }
}
