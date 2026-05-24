package com.oceanofmaya.intervalwalktrainer.home

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HomeInsightSelectionTest {
    private val weeklyGoalCard = object : HomeInsightCard {
        override val id: String = HomeInsightCardIds.WEEKLY_GOAL
        override val settingsLabelResId: Int = 0
        override val layoutResId: Int = 0

        override suspend fun isEligible(): Boolean = true

        override fun bind(view: android.view.View) = Unit

        override fun contentDescription(): String = ""
    }

    @Test
    fun `resolveSelectionState shows empty note when nothing selected`() {
        val state = HomeInsightSelection.resolveSelectionState(emptyList())

        assertFalse(state.hasUserSelection)
        assertTrue(state.showEmptyNote)
    }

    @Test
    fun `resolveSelectionState hides empty note when cards selected`() {
        val state = HomeInsightSelection.resolveSelectionState(listOf(HomeInsightCardIds.WEEKLY_GOAL))

        assertTrue(state.hasUserSelection)
        assertFalse(state.showEmptyNote)
    }

    @Test
    fun `resolveSelectedCards preserves user order and ignores unknown ids`() {
        val cards = HomeInsightSelection.resolveSelectedCards(
            allCards = listOf(weeklyGoalCard),
            enabledCardIds = listOf("unknown", HomeInsightCardIds.WEEKLY_GOAL)
        )

        assertEquals(listOf(weeklyGoalCard), cards)
    }
}
