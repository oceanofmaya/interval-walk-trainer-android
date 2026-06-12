package com.oceanofmaya.intervalwalktrainer.home

import com.oceanofmaya.intervalwalktrainer.R
import com.oceanofmaya.intervalwalktrainer.WorkoutSession
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
class LastWorkoutInsightFormatterTest {
    private val context = mock<android.content.Context>()
    private lateinit var baseFormatter: HomeInsightUiFormatter
    private lateinit var formatter: LastWorkoutInsightFormatter

    @BeforeEach
    fun setup() {
        whenever(context.getString(R.string.label_today)).thenReturn("Today")
        whenever(context.getString(R.string.label_yesterday)).thenReturn("Yesterday")
        whenever(context.getString(R.string.format_time_min, 30)).thenReturn("30 min")
        whenever(context.getString(R.string.format_last_workout_meta, "30 min", "Jan 15"))
            .thenReturn("30 min\u2002•\u2002Jan 15")
        whenever(context.getString(R.string.separator_bullet)).thenReturn("\u2002•\u2002")
        baseFormatter = HomeInsightUiFormatter(context)
        formatter = LastWorkoutInsightFormatter(context)
    }

    @Test
    fun `lastWorkoutWhenText includes completion time for today`() {
        val session = WorkoutSession(
            date = baseFormatter.todayDateString(),
            workoutType = "3-3 Japanese - 5 Rounds (30 min)",
            minutes = 30,
            timestamp = 1_700_000_000_000L
        )

        val whenText = formatter.lastWorkoutWhenText(session)

        assertEquals(true, whenText.startsWith("Today\u2002•\u2002"))
    }

    @Test
    fun `lastWorkoutWhenText omits completion time for older dates`() {
        val session = WorkoutSession(
            date = "2024-01-15",
            workoutType = "3-3 Japanese - 5 Rounds (30 min)",
            minutes = 30,
            timestamp = 1_700_000_000_000L
        )

        assertEquals("Jan 15", formatter.lastWorkoutWhenText(session))
    }

    @Test
    fun `lastWorkoutMetaText combines duration and when line`() {
        val session = WorkoutSession(
            date = "2024-01-15",
            workoutType = "3-3 Japanese - 5 Rounds (30 min)",
            minutes = 30
        )

        assertEquals(
            "30 min\u2002•\u2002Jan 15",
            formatter.lastWorkoutMetaText(session)
        )
    }
}
