package com.oceanofmaya.intervalwalktrainer.home

import android.content.Context
import com.oceanofmaya.intervalwalktrainer.R

object HomeInsightPickerCopy {
    fun helperTextResId(
        availableCardCount: Int,
        enabledCardCount: Int,
        maxEnabledCards: Int = HomeInsightPreferences.MAX_ENABLED_CARDS
    ): Int {
        val atLimit = enabledCardCount >= maxEnabledCards
        return when {
            atLimit -> R.string.body_insight_cards_at_limit
            availableCardCount > maxEnabledCards -> R.string.body_insight_cards_picker
            else -> R.string.body_insight_cards_picker_neutral
        }
    }

    fun helperText(
        context: Context,
        availableCardCount: Int,
        enabledCardCount: Int,
        maxEnabledCards: Int = HomeInsightPreferences.MAX_ENABLED_CARDS
    ): String {
        val resId = helperTextResId(availableCardCount, enabledCardCount, maxEnabledCards)
        return if (resId == R.string.body_insight_cards_picker ||
            resId == R.string.body_insight_cards_at_limit
        ) {
            context.getString(resId, maxEnabledCards)
        } else {
            context.getString(resId)
        }
    }
}
