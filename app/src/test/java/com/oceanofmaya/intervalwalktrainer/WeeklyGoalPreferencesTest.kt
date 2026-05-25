package com.oceanofmaya.intervalwalktrainer

import android.content.SharedPreferences
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Calendar

class WeeklyGoalPreferencesTest {

    @Test
    fun `loadGoalSettings migrates stored Monday week start to Sunday`() {
        val editor = mock<SharedPreferences.Editor>()
        val prefs = mockGoalPreferences(
            editor = editor,
            storedWeekStartDay = Calendar.MONDAY
        )

        val settings = WeeklyGoalPreferences.loadGoalSettings(prefs)

        assertEquals(Calendar.SUNDAY, settings.weekStartDay)
        verify(editor).putInt(WeeklyGoalPreferences.KEY_WEEKLY_GOAL_WEEK_START_DAY, Calendar.SUNDAY)
    }

    @Test
    fun `saveGoalSettings writes Sunday week start even when settings contain Monday`() {
        val editor = mock<SharedPreferences.Editor>()
        val prefs = mock<SharedPreferences>()
        whenever(prefs.edit()).thenReturn(editor)
        whenever(editor.putBoolean(any(), any())).thenReturn(editor)
        whenever(editor.putInt(any(), any())).thenReturn(editor)

        WeeklyGoalPreferences.saveGoalSettings(
            prefs,
            WeeklyGoalSettings(enabled = true, targetWorkouts = 4, targetMinutes = 120, weekStartDay = Calendar.MONDAY)
        )

        verify(editor).putInt(WeeklyGoalPreferences.KEY_WEEKLY_GOAL_WEEK_START_DAY, Calendar.SUNDAY)
    }

    private fun mockGoalPreferences(
        editor: SharedPreferences.Editor,
        storedWeekStartDay: Int
    ): SharedPreferences {
        val prefs = mock<SharedPreferences>()
        whenever(prefs.getBoolean(WeeklyGoalPreferences.KEY_WEEKLY_GOAL_ENABLED, false)).thenReturn(true)
        whenever(
            prefs.getInt(
                WeeklyGoalPreferences.KEY_WEEKLY_GOAL_TARGET_WORKOUTS,
                WeeklyGoalPreferences.DEFAULT_TARGET_WORKOUTS
            )
        ).thenReturn(WeeklyGoalPreferences.DEFAULT_TARGET_WORKOUTS)
        whenever(
            prefs.getInt(
                WeeklyGoalPreferences.KEY_WEEKLY_GOAL_TARGET_MINUTES,
                WeeklyGoalPreferences.DEFAULT_TARGET_MINUTES
            )
        ).thenReturn(WeeklyGoalPreferences.DEFAULT_TARGET_MINUTES)
        whenever(
            prefs.getInt(
                WeeklyGoalPreferences.KEY_WEEKLY_GOAL_WEEK_START_DAY,
                Calendar.SUNDAY
            )
        ).thenReturn(storedWeekStartDay)
        whenever(prefs.edit()).thenReturn(editor)
        whenever(editor.putInt(eq(WeeklyGoalPreferences.KEY_WEEKLY_GOAL_WEEK_START_DAY), eq(Calendar.SUNDAY)))
            .thenReturn(editor)
        return prefs
    }
}
