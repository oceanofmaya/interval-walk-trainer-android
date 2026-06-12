package com.oceanofmaya.intervalwalktrainer

import android.content.SharedPreferences
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class HealthConnectDiscoveryPreferencesTest {
    private val preferences = mock<SharedPreferences>()

    @Test
    fun `shouldShowWhatsNew when version is new enough and prompt not seen`() {
        whenever(
            preferences.getBoolean(HealthConnectDiscoveryPreferences.KEY_WHATS_NEW_HEALTH_CONNECT_SEEN, false)
        ).thenReturn(false)

        assertTrue(
            HealthConnectDiscoveryPreferences.shouldShowWhatsNew(
                preferences = preferences,
                currentVersionCode = HealthConnectDiscoveryPreferences.VERSION_CODE_WHATS_NEW_HEALTH_CONNECT
            )
        )
    }

    @Test
    fun `shouldShowWhatsNew is false after prompt seen`() {
        whenever(
            preferences.getBoolean(HealthConnectDiscoveryPreferences.KEY_WHATS_NEW_HEALTH_CONNECT_SEEN, false)
        ).thenReturn(true)

        assertFalse(
            HealthConnectDiscoveryPreferences.shouldShowWhatsNew(
                preferences = preferences,
                currentVersionCode = HealthConnectDiscoveryPreferences.VERSION_CODE_WHATS_NEW_HEALTH_CONNECT
            )
        )
    }

    @Test
    fun `shouldShowHistoryBanner hides when metrics already enabled`() {
        whenever(
            preferences.getBoolean(
                HealthConnectDiscoveryPreferences.KEY_HEALTH_CONNECT_HISTORY_BANNER_DISMISSED,
                false
            )
        ).thenReturn(false)

        assertFalse(
            HealthConnectDiscoveryPreferences.shouldShowHistoryBanner(
                preferences = preferences,
                metricsEnabled = true
            )
        )
    }
}
