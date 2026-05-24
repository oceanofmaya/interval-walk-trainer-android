package com.oceanofmaya.intervalwalktrainer.home

import android.content.SharedPreferences
import androidx.core.content.edit

object HomeInsightPreferences {
    const val KEY_ENABLED_CARD_IDS = "home_insight_enabled_card_ids"
    const val MAX_ENABLED_CARDS = 5

    val DEFAULT_ENABLED_CARD_IDS: List<String> = listOf(HomeInsightCardIds.WEEKLY_GOAL)

    fun loadEnabledCardIds(sharedPreferences: SharedPreferences): Set<String> {
        return loadEnabledCardIdsOrdered(sharedPreferences).toSet()
    }

    fun loadEnabledCardIdsOrdered(sharedPreferences: SharedPreferences): List<String> {
        if (!sharedPreferences.contains(KEY_ENABLED_CARD_IDS)) {
            return DEFAULT_ENABLED_CARD_IDS
        }
        val stored = sharedPreferences.getString(KEY_ENABLED_CARD_IDS, "") ?: ""
        return stored.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun saveEnabledCardIds(sharedPreferences: SharedPreferences, orderedIds: List<String>) {
        val sanitized = orderedIds
            .distinct()
            .take(MAX_ENABLED_CARDS)
        sharedPreferences.edit {
            putString(KEY_ENABLED_CARD_IDS, sanitized.joinToString(","))
        }
    }

    fun canEnableMoreCard(currentEnabledCount: Int): Boolean {
        return currentEnabledCount < MAX_ENABLED_CARDS
    }
}
