package com.oceanofmaya.intervalwalktrainer.home

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HomeInsightSelectionTest {
    private fun card(idValue: String) = object : HomeInsightCard {
        override val id: String = idValue
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
    fun `resolveSelectedCards uses registry order and ignores unknown ids`() {
        val weeklyGoalCard = card(HomeInsightCardIds.WEEKLY_GOAL)
        val currentStreakCard = card(HomeInsightCardIds.CURRENT_STREAK)

        val cards = HomeInsightSelection.resolveSelectedCards(
            allCards = listOf(weeklyGoalCard, currentStreakCard),
            enabledCardIds = listOf(
                "unknown",
                HomeInsightCardIds.CURRENT_STREAK,
                HomeInsightCardIds.WEEKLY_GOAL
            )
        )

        assertEquals(listOf(weeklyGoalCard, currentStreakCard), cards)
    }

    @Test
    fun `resolveSelectedCards displays enabled cards in saved order`() {
        val weeklyGoalCard = card(HomeInsightCardIds.WEEKLY_GOAL)
        val currentStreakCard = card(HomeInsightCardIds.CURRENT_STREAK)
        val todayCard = card(HomeInsightCardIds.TODAY)

        val cards = HomeInsightSelection.resolveSelectedCards(
            allCards = listOf(weeklyGoalCard, currentStreakCard, todayCard),
            enabledCardIds = listOf(
                HomeInsightCardIds.WEEKLY_GOAL,
                HomeInsightCardIds.TODAY
            ),
            orderedCardIds = listOf(
                HomeInsightCardIds.TODAY,
                HomeInsightCardIds.CURRENT_STREAK,
                HomeInsightCardIds.WEEKLY_GOAL
            )
        )

        assertEquals(listOf(todayCard, weeklyGoalCard), cards)
    }

    @Test
    fun `resolveSelectedCards excludes disabled cards from saved order`() {
        val weeklyGoalCard = card(HomeInsightCardIds.WEEKLY_GOAL)
        val currentStreakCard = card(HomeInsightCardIds.CURRENT_STREAK)

        val cards = HomeInsightSelection.resolveSelectedCards(
            allCards = listOf(weeklyGoalCard, currentStreakCard),
            enabledCardIds = listOf(HomeInsightCardIds.CURRENT_STREAK),
            orderedCardIds = listOf(
                HomeInsightCardIds.CURRENT_STREAK,
                HomeInsightCardIds.WEEKLY_GOAL
            )
        )

        assertEquals(listOf(currentStreakCard), cards)
    }
}
