package com.oceanofmaya.intervalwalktrainer

import com.oceanofmaya.intervalwalktrainer.R
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class WorkoutMetricsCardFormatterTest {
    private val context = mock<android.content.Context>()
    private lateinit var formatter: WorkoutMetricsCardFormatter

    @BeforeEach
    fun setup() {
        whenever(context.getString(R.string.format_heart_rate_bpm, 112)).thenReturn("112 bpm")
        whenever(context.getString(R.string.label_metrics_unavailable)).thenReturn("—")
        whenever(context.getString(R.string.label_metrics_pending_sync)).thenReturn("Pending sync")
        formatter = WorkoutMetricsCardFormatter(context)
    }

    @Test
    fun `phaseHeartRateCardValue uses pending label when no phase values exist yet`() {
        val display = formatter.phaseHeartRateCardValue(
            heartRateBpm = null,
            hasWorkoutHeartRate = true
        )

        assertTrue(display.isPlaceholder)
        assertEquals("Pending sync", display.text)
    }

    @Test
    fun `phaseHeartRateCardValue uses unavailable when peer phase is saved`() {
        val display = formatter.phaseHeartRateCardValue(
            heartRateBpm = null,
            hasWorkoutHeartRate = true,
            fastAverage = null,
            slowAverage = 112
        )

        assertTrue(display.isPlaceholder)
        assertEquals("—", display.text)
    }

    @Test
    fun `stepsCardValue returns placeholder when metrics enabled without data`() {
        val display = formatter.stepsCardValue(stepCount = null, showMetricPlaceholders = true)

        assertNotNull(display)
        assertTrue(display!!.isPlaceholder)
        assertEquals("—", display.text)
    }
}
