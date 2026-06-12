package com.oceanofmaya.intervalwalktrainer

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class WorkoutMetricsManagerTest {
    private val context = mock<android.content.Context>()
    private lateinit var manager: WorkoutMetricsManager

    @BeforeEach
    fun setup() {
        whenever(context.applicationContext).thenReturn(context)
        manager = WorkoutMetricsManager(context)
    }

    @Test
    fun `restoreFromElapsedSeconds rebuilds session start from timer elapsed time`() {
        manager.restoreFromElapsedSeconds(
            elapsedSeconds = 1_800,
            metricsEnabled = true,
            nowMillis = 3_600_000L
        )

        val snapshot = manager.snapshot()

        assertEquals(1_800_000L, snapshot?.sessionStartedAtMillis)
        assertEquals(1, snapshot?.activeIntervals?.size)
        assertEquals(1_800_000L, snapshot?.activeIntervals?.first()?.startedAtMillis)
        assertEquals(3_600_000L, snapshot?.activeIntervals?.first()?.endedAtMillis)
        assertTrue(snapshot?.running == true)
    }
}
