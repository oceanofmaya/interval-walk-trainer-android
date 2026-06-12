package com.oceanofmaya.intervalwalktrainer.home

import com.oceanofmaya.intervalwalktrainer.R
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@ExtendWith(MockitoExtension::class)
class HomeInsightUiFormatterTest {
    private val context = mock<android.content.Context>()
    private lateinit var formatter: HomeInsightUiFormatter
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    @BeforeEach
    fun setup() {
        whenever(context.getString(R.string.format_day, 1)).thenReturn("1 day")
        whenever(context.getString(R.string.format_days, 3)).thenReturn("3 days")
        whenever(context.getString(R.string.label_today)).thenReturn("Today")
        whenever(context.getString(R.string.label_yesterday)).thenReturn("Yesterday")
        whenever(context.getString(R.string.format_workout_count_singular)).thenReturn("1 workout")
        whenever(context.getString(R.string.format_workout_count, 2)).thenReturn("2 workouts")
        whenever(context.getString(R.string.format_time_min, 28)).thenReturn("28 min")
        whenever(context.getString(R.string.format_time_min, 30)).thenReturn("30 min")
        whenever(context.getString(R.string.format_today_summary, "2 workouts", "28 min"))
            .thenReturn("2 workouts\u2002•\u200228 min")
        formatter = HomeInsightUiFormatter(context)
    }

    @Test
    fun `streakDaysText uses singular for one day`() {
        assertEquals("1 day", formatter.streakDaysText(1))
    }

    @Test
    fun `streakDaysText uses plural for multiple days`() {
        assertEquals("3 days", formatter.streakDaysText(3))
    }

    @Test
    fun `streakProgressPercent calculates toward longest streak`() {
        assertEquals(50, formatter.streakProgressPercent(2, 4))
        assertEquals(100, formatter.streakProgressPercent(5, 4))
        assertEquals(0, formatter.streakProgressPercent(2, 0))
    }

    @Test
    fun `relativeDateLabel returns Today for today date`() {
        assertEquals("Today", formatter.relativeDateLabel(formatter.todayDateString()))
    }

    @Test
    fun `relativeDateLabel returns Yesterday for yesterday date`() {
        val yesterday = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_MONTH, -1)
        }
        val yesterdayDate = dateFormat.format(yesterday.time)
        assertEquals("Yesterday", formatter.relativeDateLabel(yesterdayDate))
    }

    @Test
    fun `todaySummaryText combines workouts and minutes`() {
        assertEquals("2 workouts\u2002•\u200228 min", formatter.todaySummaryText(2, 28))
    }

}
