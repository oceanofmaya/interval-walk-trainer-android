package com.oceanofmaya.intervalwalktrainer.home

import android.content.SharedPreferences
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class HomeInsightPreferencesTest {
    @Test
    fun `loadEnabledCardIds returns default when preference key is absent`() {
        val prefs = mock<SharedPreferences>()
        whenever(prefs.contains(HomeInsightPreferences.KEY_ENABLED_CARD_IDS)).thenReturn(false)

        assertEquals(
            HomeInsightPreferences.DEFAULT_ENABLED_CARD_IDS,
            HomeInsightPreferences.loadEnabledCardIdsOrdered(prefs)
        )
    }

    @Test
    fun `loadEnabledCardIds preserves explicit empty selection`() {
        val prefs = mock<SharedPreferences>()
        whenever(prefs.contains(HomeInsightPreferences.KEY_ENABLED_CARD_IDS)).thenReturn(true)
        whenever(prefs.getString(HomeInsightPreferences.KEY_ENABLED_CARD_IDS, "")).thenReturn("")

        assertTrue(HomeInsightPreferences.loadEnabledCardIdsOrdered(prefs).isEmpty())
    }

    @Test
    fun `saveEnabledCardIds trims duplicates and enforces max`() {
        val editor = mock<SharedPreferences.Editor>()
        val prefs = mock<SharedPreferences>()
        whenever(prefs.edit()).thenReturn(editor)
        whenever(editor.putString(org.mockito.kotlin.any(), org.mockito.kotlin.any())).thenReturn(editor)

        val ids = List(HomeInsightPreferences.MAX_ENABLED_CARDS + 2) { "card_$it" }
        HomeInsightPreferences.saveEnabledCardIds(prefs, ids)

        verify(editor).putString(
            HomeInsightPreferences.KEY_ENABLED_CARD_IDS,
            ids.take(HomeInsightPreferences.MAX_ENABLED_CARDS).joinToString(",")
        )
    }

    @Test
    fun `canEnableMoreCard respects max limit`() {
        assertTrue(HomeInsightPreferences.canEnableMoreCard(0))
        assertTrue(HomeInsightPreferences.canEnableMoreCard(HomeInsightPreferences.MAX_ENABLED_CARDS - 1))
        assertFalse(HomeInsightPreferences.canEnableMoreCard(HomeInsightPreferences.MAX_ENABLED_CARDS))
    }
}
