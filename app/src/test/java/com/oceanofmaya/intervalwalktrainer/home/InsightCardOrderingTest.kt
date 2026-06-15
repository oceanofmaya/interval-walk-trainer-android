package com.oceanofmaya.intervalwalktrainer.home

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class InsightCardOrderingTest {
    private val order = listOf("weekly_goal", "current_streak", "today", "last_workout")

    @Test
    fun `moveUp swaps enabled card with previous enabled neighbour`() {
        val enabled = setOf("weekly_goal", "current_streak", "today")

        val result = InsightCardOrdering.moveUp(order, enabled, "today")

        assertEquals(listOf("weekly_goal", "today", "current_streak", "last_workout"), result)
    }

    @Test
    fun `moveUp skips disabled cards between enabled cards`() {
        val enabled = setOf("weekly_goal", "today")

        val result = InsightCardOrdering.moveUp(order, enabled, "today")

        // current_streak is disabled, so today swaps with weekly_goal, keeping current_streak in place.
        assertEquals(listOf("today", "current_streak", "weekly_goal", "last_workout"), result)
    }

    @Test
    fun `moveUp on first enabled card is a no-op`() {
        val enabled = setOf("weekly_goal", "current_streak")

        val result = InsightCardOrdering.moveUp(order, enabled, "weekly_goal")

        assertEquals(order, result)
    }

    @Test
    fun `moveDown swaps enabled card with next enabled neighbour`() {
        val enabled = setOf("weekly_goal", "current_streak", "today")

        val result = InsightCardOrdering.moveDown(order, enabled, "weekly_goal")

        assertEquals(listOf("current_streak", "weekly_goal", "today", "last_workout"), result)
    }

    @Test
    fun `moveDown on last enabled card is a no-op`() {
        val enabled = setOf("weekly_goal", "today")

        val result = InsightCardOrdering.moveDown(order, enabled, "today")

        assertEquals(order, result)
    }

    @Test
    fun `move ignores cards that are not enabled`() {
        val enabled = setOf("weekly_goal")

        assertEquals(order, InsightCardOrdering.moveUp(order, enabled, "today"))
        assertEquals(order, InsightCardOrdering.moveDown(order, enabled, "today"))
    }
}
