package com.oceanofmaya.intervalwalktrainer

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.view.ContextThemeWrapper

/**
 * Insight cards are inflated again when the home screen resumes. Locking the
 * phone or switching apps can leave the activity's resource configuration on
 * the system night mode while AppCompat still treats the chosen light theme as
 * applied, so it does not recreate the activity. Views inflated earlier keep
 * their original colors; newly inflated insight cards then read night colors.
 */
object ThemeNightModeSync {
    fun themedContext(context: Context): Context {
        val activity = context.findAppCompatActivity()
        val expectedNightFlag = activity?.let { expectedNightFlagFor(it) }
        return when {
            activity == null -> context
            expectedNightFlag == null -> activity
            else -> contextAlignedToNightMode(activity, expectedNightFlag)
        }
    }

    internal fun expectedNightModeFlag(configuredNightMode: Int, systemNightModeFlag: Int): Int? {
        return when (configuredNightMode) {
            AppCompatDelegate.MODE_NIGHT_YES -> Configuration.UI_MODE_NIGHT_YES
            AppCompatDelegate.MODE_NIGHT_NO -> Configuration.UI_MODE_NIGHT_NO
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
            AppCompatDelegate.MODE_NIGHT_UNSPECIFIED -> systemNightModeFlag
            else -> null
        }
    }

    internal fun uiModeWithNightFlag(uiMode: Int, nightFlag: Int): Int {
        return (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or nightFlag
    }

    private fun expectedNightFlagFor(activity: AppCompatActivity): Int? {
        return expectedNightModeFlag(
            configuredNightMode = configuredNightMode(activity),
            systemNightModeFlag = nightModeFlag(activity.applicationContext.resources.configuration.uiMode)
        )
    }

    private fun contextAlignedToNightMode(activity: AppCompatActivity, expectedNightFlag: Int): Context {
        if (nightModeFlag(activity.resources.configuration.uiMode) == expectedNightFlag) {
            return activity
        }
        val corrected = Configuration(activity.resources.configuration)
        corrected.uiMode = uiModeWithNightFlag(corrected.uiMode, expectedNightFlag)
        @Suppress("DEPRECATION")
        activity.resources.updateConfiguration(Configuration(corrected), activity.resources.displayMetrics)
        return ContextThemeWrapper(
            activity.createConfigurationContext(Configuration(corrected)),
            R.style.Theme_IntervalWalkTrainer
        )
    }

    private fun configuredNightMode(activity: AppCompatActivity): Int {
        val localNightMode = activity.delegate.localNightMode
        return if (localNightMode != AppCompatDelegate.MODE_NIGHT_UNSPECIFIED) {
            localNightMode
        } else {
            AppCompatDelegate.getDefaultNightMode()
        }
    }

    private fun nightModeFlag(uiMode: Int): Int = uiMode and Configuration.UI_MODE_NIGHT_MASK

    private fun Context.findAppCompatActivity(): AppCompatActivity? {
        var current: Context? = this
        while (current != null) {
            when (val candidate = current) {
                is AppCompatActivity -> return candidate
                is ContextWrapper -> {
                    val base = candidate.baseContext
                    current = if (base == candidate) null else base
                }
                else -> current = null
            }
        }
        return null
    }
}
