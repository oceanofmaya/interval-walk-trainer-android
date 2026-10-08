package com.oceanofmaya.intervalwalktrainer

import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ThemeNightModeSyncTest {
    @Test
    fun `forced light mode stays light when the system is dark`() {
        val flag = ThemeNightModeSync.expectedNightModeFlag(
            configuredNightMode = AppCompatDelegate.MODE_NIGHT_NO,
            systemNightModeFlag = Configuration.UI_MODE_NIGHT_YES
        )

        assertEquals(Configuration.UI_MODE_NIGHT_NO, flag)
    }

    @Test
    fun `forced dark mode stays dark when the system is light`() {
        val flag = ThemeNightModeSync.expectedNightModeFlag(
            configuredNightMode = AppCompatDelegate.MODE_NIGHT_YES,
            systemNightModeFlag = Configuration.UI_MODE_NIGHT_NO
        )

        assertEquals(Configuration.UI_MODE_NIGHT_YES, flag)
    }

    @Test
    fun `follow system uses the system night flag`() {
        assertEquals(
            Configuration.UI_MODE_NIGHT_YES,
            ThemeNightModeSync.expectedNightModeFlag(
                configuredNightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
                systemNightModeFlag = Configuration.UI_MODE_NIGHT_YES
            )
        )
        assertEquals(
            Configuration.UI_MODE_NIGHT_NO,
            ThemeNightModeSync.expectedNightModeFlag(
                configuredNightMode = AppCompatDelegate.MODE_NIGHT_UNSPECIFIED,
                systemNightModeFlag = Configuration.UI_MODE_NIGHT_NO
            )
        )
    }

    @Test
    fun `automatic night modes are left to AppCompat`() {
        assertNull(
            ThemeNightModeSync.expectedNightModeFlag(
                configuredNightMode = AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY,
                systemNightModeFlag = Configuration.UI_MODE_NIGHT_YES
            )
        )
    }

    @Test
    fun `replacing the night flag keeps the rest of uiMode`() {
        val uiMode = Configuration.UI_MODE_TYPE_NORMAL or Configuration.UI_MODE_NIGHT_YES

        val corrected = ThemeNightModeSync.uiModeWithNightFlag(
            uiMode = uiMode,
            nightFlag = Configuration.UI_MODE_NIGHT_NO
        )

        assertEquals(Configuration.UI_MODE_TYPE_NORMAL, corrected and Configuration.UI_MODE_TYPE_MASK)
        assertEquals(Configuration.UI_MODE_NIGHT_NO, corrected and Configuration.UI_MODE_NIGHT_MASK)
    }
}
