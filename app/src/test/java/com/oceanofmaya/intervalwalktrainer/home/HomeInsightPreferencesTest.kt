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

    @Test
    fun `loadCardOrderIds returns null when order key absent`() {
        val prefs = mock<SharedPreferences>()
        whenever(prefs.contains(HomeInsightPreferences.KEY_CARD_ORDER_IDS)).thenReturn(false)

        assertEquals(null, HomeInsightPreferences.loadCardOrderIds(prefs))
    }

    @Test
    fun `loadCardOrderIds parses stored order`() {
        val prefs = mock<SharedPreferences>()
        whenever(prefs.contains(HomeInsightPreferences.KEY_CARD_ORDER_IDS)).thenReturn(true)
        whenever(prefs.getString(HomeInsightPreferences.KEY_CARD_ORDER_IDS, ""))
            .thenReturn("today, weekly_goal ,last_workout")

        assertEquals(
            listOf("today", "weekly_goal", "last_workout"),
            HomeInsightPreferences.loadCardOrderIds(prefs)
        )
    }

    @Test
    fun `saveCardOrderIds stores distinct comma separated order`() {
        val editor = mock<SharedPreferences.Editor>()
        val prefs = mock<SharedPreferences>()
        whenever(prefs.edit()).thenReturn(editor)
        whenever(editor.putString(org.mockito.kotlin.any(), org.mockito.kotlin.any())).thenReturn(editor)

        HomeInsightPreferences.saveCardOrderIds(prefs, listOf("today", "weekly_goal", "today"))

        verify(editor).putString(
            HomeInsightPreferences.KEY_CARD_ORDER_IDS,
            "today,weekly_goal"
        )
    }

    @Test
    fun `resolveCardOrder uses registry order when stored order missing`() {
        val registry = listOf("weekly_goal", "current_streak", "today", "last_workout")

        assertEquals(registry, HomeInsightPreferences.resolveCardOrder(registry, null))
    }

    @Test
    fun `resolveCardOrder respects stored order and ignores unknown ids`() {
        val registry = listOf("weekly_goal", "current_streak", "today", "last_workout")
        val stored = listOf("today", "unknown", "weekly_goal")

        assertEquals(
            listOf("today", "weekly_goal", "current_streak", "last_workout"),
            HomeInsightPreferences.resolveCardOrder(registry, stored)
        )
    }

    @Test
    fun `resolveCardOrder appends newly introduced registry cards in registry order`() {
        val registry = listOf("weekly_goal", "current_streak", "today", "last_workout")
        val stored = listOf("today", "weekly_goal")

        assertEquals(
            listOf("today", "weekly_goal", "current_streak", "last_workout"),
            HomeInsightPreferences.resolveCardOrder(registry, stored)
        )
    }
}
