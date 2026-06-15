package com.oceanofmaya.intervalwalktrainer.home

import android.content.SharedPreferences
import androidx.core.content.edit

object HomeInsightPreferences {
    const val KEY_ENABLED_CARD_IDS = "home_insight_enabled_card_ids"
    const val KEY_CARD_ORDER_IDS = "home_insight_card_order_ids"
    const val MAX_ENABLED_CARDS = 5

    val DEFAULT_ENABLED_CARD_IDS: List<String> = listOf(HomeInsightCardIds.WEEKLY_GOAL)

    fun loadEnabledCardIds(sharedPreferences: SharedPreferences): Set<String> {
        return loadEnabledCardIdsOrdered(sharedPreferences).toSet()
    }

    fun loadEnabledCardIdsOrdered(sharedPreferences: SharedPreferences): List<String> {
        if (!sharedPreferences.contains(KEY_ENABLED_CARD_IDS)) {
            return DEFAULT_ENABLED_CARD_IDS
        }
        return parseIds(sharedPreferences.getString(KEY_ENABLED_CARD_IDS, "") ?: "")
    }

    fun saveEnabledCardIds(sharedPreferences: SharedPreferences, orderedIds: List<String>) {
        val sanitized = orderedIds
            .distinct()
            .take(MAX_ENABLED_CARDS)
        sharedPreferences.edit {
            putString(KEY_ENABLED_CARD_IDS, sanitized.joinToString(","))
        }
    }

    /**
     * Returns the user's stored card order, or null when no custom order has been saved yet.
     * A null result signals callers to fall back to registry order.
     */
    fun loadCardOrderIds(sharedPreferences: SharedPreferences): List<String>? {
        if (!sharedPreferences.contains(KEY_CARD_ORDER_IDS)) {
            return null
        }
        return parseIds(sharedPreferences.getString(KEY_CARD_ORDER_IDS, "") ?: "")
    }

    fun saveCardOrderIds(sharedPreferences: SharedPreferences, orderedIds: List<String>) {
        sharedPreferences.edit {
            putString(KEY_CARD_ORDER_IDS, orderedIds.distinct().joinToString(","))
        }
    }

    /**
     * Combines the registry's known cards with the user's stored order so that:
     * - the stored order is respected,
     * - unknown/removed card IDs are dropped,
     * - newly introduced registry cards are appended in registry order.
     */
    fun resolveCardOrder(
        registryOrderedIds: List<String>,
        storedOrder: List<String>?
    ): List<String> {
        val registrySet = registryOrderedIds.toSet()
        val base = (storedOrder ?: emptyList())
            .filter { it in registrySet }
            .distinct()
        val baseSet = base.toSet()
        val appended = registryOrderedIds.filter { it !in baseSet }
        return base + appended
    }

    fun canEnableMoreCard(currentEnabledCount: Int): Boolean {
        return currentEnabledCount < MAX_ENABLED_CARDS
    }

    private fun parseIds(raw: String): List<String> {
        return raw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
}
