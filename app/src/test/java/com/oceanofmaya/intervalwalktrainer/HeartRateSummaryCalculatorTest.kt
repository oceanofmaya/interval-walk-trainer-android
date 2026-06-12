package com.oceanofmaya.intervalwalktrainer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class HeartRateSummaryCalculatorTest {
    @Test
    fun `summarize returns average min and max`() {
        val summary = HeartRateSummaryCalculator.summarize(listOf(100, 120, 140))

        assertEquals(120, summary?.averageBpm)
        assertEquals(100, summary?.minBpm)
        assertEquals(140, summary?.maxBpm)
    }

    @Test
    fun `summarize returns null for empty or invalid samples`() {
        assertNull(HeartRateSummaryCalculator.summarize(emptyList()))
        assertNull(HeartRateSummaryCalculator.summarize(listOf(0, -1)))
    }
}
