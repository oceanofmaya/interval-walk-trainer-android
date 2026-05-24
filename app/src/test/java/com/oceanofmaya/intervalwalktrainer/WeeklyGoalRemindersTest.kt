package com.oceanofmaya.intervalwalktrainer

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WeeklyGoalRemindersTest {
    @Test
    fun `remindersAvailable requires enabled goal with at least one target`() {
        assertFalse(WeeklyGoalSettings(enabled = false, targetWorkouts = 3).remindersAvailable())
        assertFalse(WeeklyGoalSettings(enabled = true, targetWorkouts = 0, targetMinutes = 0).remindersAvailable())
        assertTrue(WeeklyGoalSettings(enabled = true, targetWorkouts = 2, targetMinutes = 0).remindersAvailable())
        assertTrue(WeeklyGoalSettings(enabled = true, targetWorkouts = 0, targetMinutes = 30).remindersAvailable())
    }

    @Test
    fun `canScheduleWeeklyReminders returns false when goal is inactive`() {
        val reminderSettings = WeeklyReminderSettings(enabled = true)
        val inactiveGoal = WeeklyGoalSettings(enabled = false, targetWorkouts = 3)

        assertFalse(canScheduleWeeklyReminders(inactiveGoal, reminderSettings, isGoalMet = false))
    }

    @Test
    fun `canScheduleWeeklyReminders returns false when goal has no targets`() {
        val reminderSettings = WeeklyReminderSettings(enabled = true)
        val emptyGoal = WeeklyGoalSettings(enabled = true, targetWorkouts = 0, targetMinutes = 0)

        assertFalse(canScheduleWeeklyReminders(emptyGoal, reminderSettings, isGoalMet = false))
    }

    @Test
    fun `canScheduleWeeklyReminders returns false when reminders disabled`() {
        val goalSettings = WeeklyGoalSettings(enabled = true, targetWorkouts = 3)
        val reminderSettings = WeeklyReminderSettings(enabled = false)

        assertFalse(canScheduleWeeklyReminders(goalSettings, reminderSettings, isGoalMet = false))
    }

    @Test
    fun `canScheduleWeeklyReminders returns true for active goal with reminders enabled`() {
        val goalSettings = WeeklyGoalSettings(enabled = true, targetWorkouts = 3)
        val reminderSettings = WeeklyReminderSettings(enabled = true)

        assertTrue(canScheduleWeeklyReminders(goalSettings, reminderSettings, isGoalMet = false))
    }

    @Test
    fun `canScheduleWeeklyReminders returns false when goal met and pause enabled`() {
        val goalSettings = WeeklyGoalSettings(enabled = true, targetWorkouts = 3)
        val reminderSettings = WeeklyReminderSettings(enabled = true, pauseWhenGoalMet = true)

        assertFalse(canScheduleWeeklyReminders(goalSettings, reminderSettings, isGoalMet = true))
    }

    @Test
    fun `canScheduleWeeklyReminders returns true when goal met and pause disabled`() {
        val goalSettings = WeeklyGoalSettings(enabled = true, targetWorkouts = 3)
        val reminderSettings = WeeklyReminderSettings(enabled = true, pauseWhenGoalMet = false)

        assertTrue(canScheduleWeeklyReminders(goalSettings, reminderSettings, isGoalMet = true))
    }
}
