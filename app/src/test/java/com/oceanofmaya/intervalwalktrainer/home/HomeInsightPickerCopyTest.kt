package com.oceanofmaya.intervalwalktrainer.home

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import com.oceanofmaya.intervalwalktrainer.R

class HomeInsightPickerCopyTest {
    @Test
    fun `helperTextResId uses neutral copy when choices fit under cap`() {
        val resId = HomeInsightPickerCopy.helperTextResId(
            availableCardCount = 4,
            enabledCardCount = 2,
            maxEnabledCards = 5
        )

        assertEquals(R.string.body_insight_cards_picker_neutral, resId)
    }

    @Test
    fun `helperTextResId uses numbered picker when more choices than cap`() {
        val resId = HomeInsightPickerCopy.helperTextResId(
            availableCardCount = 6,
            enabledCardCount = 2,
            maxEnabledCards = 5
        )

        assertEquals(R.string.body_insight_cards_picker, resId)
    }

    @Test
    fun `helperTextResId uses at limit copy when enabled count reaches cap`() {
        val resId = HomeInsightPickerCopy.helperTextResId(
            availableCardCount = 4,
            enabledCardCount = 5,
            maxEnabledCards = 5
        )

        assertEquals(R.string.body_insight_cards_at_limit, resId)
    }
}
