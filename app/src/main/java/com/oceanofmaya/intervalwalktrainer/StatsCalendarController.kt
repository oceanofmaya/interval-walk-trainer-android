package com.oceanofmaya.intervalwalktrainer

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.oceanofmaya.intervalwalktrainer.databinding.ActivityStatsBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class StatsCalendarController(
    private val activity: AppCompatActivity,
    private val binding: ActivityStatsBinding,
    private val dateFormat: SimpleDateFormat,
    private val accentColorProvider: () -> Int,
    private val onWorkoutDateSelected: (String) -> Unit
) {
    fun setupCalendarGrid(year: Int, month: Int, workoutDates: Set<String>) {
        val calendar = Calendar.getInstance()
        val todayCalendar = Calendar.getInstance()
        val todayYear = todayCalendar.get(Calendar.YEAR)
        val todayMonth = todayCalendar.get(Calendar.MONTH)
        val todayDay = todayCalendar.get(Calendar.DAY_OF_MONTH)

        calendar.set(year, month, 1)
        binding.calendarGrid.removeAllViews()

        val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val cellSize = calendarCellSize()
        addDayHeaders(cellSize)
        addLeadingEmptyCells(firstDayOfWeek, cellSize)

        for (day in 1..daysInMonth) {
            addDayCell(
                calendar = calendar,
                day = day,
                cellSize = cellSize,
                today = CalendarDate(todayYear, todayMonth, todayDay),
                workoutDates = workoutDates
            )
        }
    }

    private fun calendarCellSize(): CalendarCellSize {
        val displayMetrics = activity.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val horizontalPadding = (48 * displayMetrics.density).toInt()
        return CalendarCellSize(
            width = (screenWidth - horizontalPadding) / DAYS_PER_WEEK,
            height = (40 * displayMetrics.density).toInt()
        )
    }

    private fun addDayHeaders(cellSize: CalendarCellSize) {
        arrayOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
            val textView = TextView(activity).apply {
                text = day
                textSize = 12f
                gravity = Gravity.CENTER
                setTextColor(activity.getColor(R.color.text_secondary))
                layoutParams = gridLayoutParams(cellSize)
            }
            binding.calendarGrid.addView(textView)
        }
    }

    private fun addLeadingEmptyCells(firstDayOfWeek: Int, cellSize: CalendarCellSize) {
        repeat((firstDayOfWeek - 1).coerceAtLeast(0)) {
            binding.calendarGrid.addView(
                TextView(activity).apply {
                    layoutParams = gridLayoutParams(cellSize)
                }
            )
        }
    }

    private fun addDayCell(
        calendar: Calendar,
        day: Int,
        cellSize: CalendarCellSize,
        today: CalendarDate,
        workoutDates: Set<String>
    ) {
        calendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), day)
        val dateString = dateFormat.format(calendar.time)
        val isToday = today.year == calendar.get(Calendar.YEAR) &&
            today.month == calendar.get(Calendar.MONTH) &&
            today.day == day
        val hasWorkout = dateString in workoutDates

        val textView = TextView(activity).apply {
            text = String.format(Locale.US, "%d", day)
            gravity = Gravity.CENTER
            setTextColor(activity.getColor(R.color.text_primary))
            layoutParams = gridLayoutParams(cellSize)
        }
        styleDayCell(textView, isToday, hasWorkout)
        if (hasWorkout) {
            textView.setOnClickListener { view ->
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onWorkoutDateSelected(dateString)
            }
            textView.isClickable = true
            textView.isFocusable = true
        }
        binding.calendarGrid.addView(textView)
    }

    private fun styleDayCell(textView: TextView, isToday: Boolean, hasWorkout: Boolean) {
        when {
            isToday && hasWorkout -> {
                textView.background = createCalendarDayBackground(
                    fillColor = accentColorProvider(),
                    strokeColor = todayOutlineColor()
                )
                textView.setTextColor(activity.getColor(R.color.white))
            }
            isToday -> {
                textView.background = createCalendarDayBackground(
                    fillColor = null,
                    strokeColor = todayOutlineColor()
                )
            }
            hasWorkout -> {
                textView.background = createCalendarDayBackground(fillColor = accentColorProvider())
                textView.setTextColor(activity.getColor(R.color.white))
            }
        }
    }

    private fun gridLayoutParams(cellSize: CalendarCellSize): android.widget.GridLayout.LayoutParams {
        return android.widget.GridLayout.LayoutParams().apply {
            width = cellSize.width
            height = cellSize.height
        }
    }

    private fun createCalendarDayBackground(fillColor: Int?, strokeColor: Int? = null): LayerDrawable {
        val layers = mutableListOf<GradientDrawable>()
        if (strokeColor != null) {
            layers.add(ovalDrawable(fillColor = Color.TRANSPARENT, strokeColor = strokeColor, strokeWidthDp = 2.5f))
        }
        if (fillColor != null) {
            layers.add(ovalDrawable(fillColor = fillColor))
        }

        return LayerDrawable(layers.toTypedArray()).apply {
            if (strokeColor != null && fillColor != null) {
                setLayerSize(0, dpToPx(TODAY_OUTER_SIZE_DP), dpToPx(TODAY_OUTER_SIZE_DP))
                setLayerGravity(0, Gravity.CENTER)
                setLayerSize(1, dpToPx(TODAY_INNER_SIZE_DP), dpToPx(TODAY_INNER_SIZE_DP))
                setLayerGravity(1, Gravity.CENTER)
            } else {
                setLayerSize(0, dpToPx(DAY_DOT_SIZE_DP), dpToPx(DAY_DOT_SIZE_DP))
                setLayerGravity(0, Gravity.CENTER)
            }
        }
    }

    private fun ovalDrawable(
        fillColor: Int,
        strokeColor: Int? = null,
        strokeWidthDp: Float = 0f
    ): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(fillColor)
            if (strokeColor != null) {
                setStroke(dpToPx(strokeWidthDp), strokeColor)
            }
        }
    }

    private fun todayOutlineColor(): Int {
        val isDarkMode = (activity.resources.configuration.uiMode and
            android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
        return if (isDarkMode) Color.WHITE else Color.BLACK
    }

    private fun dpToPx(valueDp: Float): Int {
        return (valueDp * activity.resources.displayMetrics.density).toInt()
    }

    private data class CalendarDate(val year: Int, val month: Int, val day: Int)
    private data class CalendarCellSize(val width: Int, val height: Int)

    companion object {
        private const val DAYS_PER_WEEK = 7
        private const val TODAY_OUTER_SIZE_DP = 34f
        private const val TODAY_INNER_SIZE_DP = 31f
        private const val DAY_DOT_SIZE_DP = 32f
    }
}
