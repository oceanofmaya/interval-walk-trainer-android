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
class WorkoutMetricsUiFormatterTest {
    private val context = mock<android.content.Context>()
    private lateinit var formatter: WorkoutMetricsUiFormatter

    @BeforeEach
    fun setup() {
        whenever(context.getString(R.string.separator_bullet)).thenReturn(" • ")
        whenever(context.getString(R.string.format_fast_phase_heart_rate, "—"))
            .thenReturn("Fast (avg) —")
        whenever(context.getString(R.string.format_slow_phase_heart_rate, "112 bpm"))
            .thenReturn("Slow (avg) 112 bpm")
        whenever(context.getString(R.string.format_heart_rate_bpm, 112)).thenReturn("112 bpm")
        whenever(context.getString(R.string.label_metrics_unavailable)).thenReturn("—")
        whenever(context.getString(R.string.label_metrics_pending_sync)).thenReturn("Pending sync")
        whenever(context.getString(R.string.format_average_heart_rate_placeholder, "—")).thenReturn("Avg —")
        whenever(context.getString(R.string.format_heart_rate_range_placeholder, "—")).thenReturn("Range —")
        whenever(context.getString(R.string.format_steps, "—")).thenReturn("— steps")
        formatter = WorkoutMetricsUiFormatter(context)
    }

    @Test
    fun `phaseMetricsText shows unavailable fast when only slow is saved`() {
        val session = WorkoutSession(
            date = "2026-06-07",
            workoutType = "3-3 Japanese - 5 Rounds",
            minutes = 30,
            averageHeartRateBpm = 118,
            slowPhaseAverageHeartRateBpm = 112,
            formulaSnapshotJson = "180,180,5,0,0"
        )

        val text = formatter.phaseMetricsText(session)

        assertNotNull(text)
        assertTrue(text!!.contains("Fast (avg) —"))
        assertTrue(text.contains("Slow (avg) 112 bpm"))
    }

    @Test
    fun `detailedMetricItems shows placeholders for missing steps and heart rate when enabled`() {
        val session = WorkoutSession(
            date = "2026-06-07",
            workoutType = "3-3 Japanese - 5 Rounds",
            minutes = 30,
            formulaSnapshotJson = "180,180,5,0,0"
        )

        val items = formatter.detailedMetricItems(session, showMetricPlaceholders = true)

        assertEquals(3, items.size)
        assertEquals("—", items[0].text)
        assertEquals("Avg — • Range —", items[1].text)
        assertNotNull(items[2].text)
    }
}
