package com.oceanofmaya.intervalwalktrainer

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.edit

object HealthConnectDiscoveryPreferences {
    const val KEY_WHATS_NEW_HEALTH_CONNECT_SEEN = "whats_new_health_connect_seen"
    const val KEY_HEALTH_CONNECT_HISTORY_BANNER_DISMISSED = "health_connect_history_banner_dismissed"
    const val VERSION_CODE_WHATS_NEW_HEALTH_CONNECT = 71

    fun shouldShowWhatsNew(
        preferences: SharedPreferences,
        currentVersionCode: Int
    ): Boolean {
        if (currentVersionCode < VERSION_CODE_WHATS_NEW_HEALTH_CONNECT) return false
        return !preferences.getBoolean(KEY_WHATS_NEW_HEALTH_CONNECT_SEEN, false)
    }

    fun shouldShowHistoryBanner(
        preferences: SharedPreferences,
        metricsEnabled: Boolean
    ): Boolean {
        if (metricsEnabled) return false
        return !preferences.getBoolean(KEY_HEALTH_CONNECT_HISTORY_BANNER_DISMISSED, false)
    }

    fun markWhatsNewSeen(preferences: SharedPreferences) {
        preferences.edit { putBoolean(KEY_WHATS_NEW_HEALTH_CONNECT_SEEN, true) }
    }

    fun markHistoryBannerDismissed(preferences: SharedPreferences) {
        preferences.edit { putBoolean(KEY_HEALTH_CONNECT_HISTORY_BANNER_DISMISSED, true) }
    }

    fun appVersionCode(context: Context): Int {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode.toInt()
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode
        }
    }
}
