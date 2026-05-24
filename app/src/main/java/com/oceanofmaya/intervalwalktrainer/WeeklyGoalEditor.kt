package com.oceanofmaya.intervalwalktrainer

import android.Manifest
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class WeeklyGoalEditor(
    private val activity: AppCompatActivity,
    private val sharedPreferences: SharedPreferences,
    private val accentColorProvider: () -> Int,
    private val onSaved: () -> Unit
) {
    fun show() {
        val bottomSheetDialog = BottomSheetDialog(activity)
        val view = LayoutInflater.from(activity).inflate(
            R.layout.bottom_sheet_weekly_goal,
            FrameLayout(activity),
            false
        )
        view.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        bottomSheetDialog.setContentView(view)
        configureBottomSheet(bottomSheetDialog, view)

        val state = WeeklyGoalSheetState(
            goalSettings = WeeklyGoalPreferences.loadGoalSettings(sharedPreferences),
            reminderSettings = WeeklyGoalPreferences.loadReminderSettings(sharedPreferences)
        )
        val sheetViews = WeeklyGoalSheetViews.from(view)
        applyAccentTints(sheetViews)
        val updateSheetValues = { updateSheetValues(sheetViews, state) }

        wireGoalSwitches(sheetViews, state, updateSheetValues)
        wireDayChecks(sheetViews, state, updateSheetValues)
        wireTargetButtons(sheetViews, state, updateSheetValues)
        wireReminderTimeButtons(sheetViews, state, updateSheetValues)
        wireSheetActions(sheetViews, state, updateSheetValues, bottomSheetDialog)

        updateSheetValues()
        bottomSheetDialog.show()
    }

    private fun applyAccentTints(sheetViews: WeeklyGoalSheetViews) {
        val accent = accentColorProvider()
        val thumbTint = switchThumbTint(accent)
        val trackTint = switchTrackTint(accent)
        listOf(
            sheetViews.goalSwitch,
            sheetViews.reminderSwitch,
            sheetViews.pauseSwitch
        ).forEach { switch ->
            switch.thumbTintList = thumbTint
            switch.trackTintList = trackTint
        }
        sheetViews.dayChecks.values.forEach { checkBox ->
            checkBox.buttonTintList = switchThumbTint(accent)
        }
        sheetViews.resetButton.setTextColor(accent)
        sheetViews.saveButton.backgroundTintList = android.content.res.ColorStateList.valueOf(accent)
    }

    private fun switchThumbTint(accent: Int): android.content.res.ColorStateList {
        return android.content.res.ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf()
            ),
            intArrayOf(accent, ContextCompat.getColor(activity, android.R.color.darker_gray))
        )
    }

    private fun switchTrackTint(accent: Int): android.content.res.ColorStateList {
        val accentWithAlpha = android.graphics.Color.argb(
            (255 * SWITCH_TRACK_ALPHA).toInt(),
            android.graphics.Color.red(accent),
            android.graphics.Color.green(accent),
            android.graphics.Color.blue(accent)
        )
        return android.content.res.ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf()
            ),
            intArrayOf(accentWithAlpha, ContextCompat.getColor(activity, R.color.stroke_light))
        )
    }

    private fun wireGoalSwitches(
        sheetViews: WeeklyGoalSheetViews,
        state: WeeklyGoalSheetState,
        updateSheetValues: () -> Unit
    ) {
        sheetViews.goalSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            hapticSelection(buttonView)
            state.goalSettings = state.goalSettings.copy(enabled = isChecked)
            updateSheetValues()
        }
        sheetViews.reminderSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            hapticSelection(buttonView)
            state.reminderSettings = state.reminderSettings.copy(enabled = isChecked)
        }
        sheetViews.pauseSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
            hapticSelection(buttonView)
            state.reminderSettings = state.reminderSettings.copy(pauseWhenGoalMet = isChecked)
        }
    }

    private fun wireDayChecks(
        sheetViews: WeeklyGoalSheetViews,
        state: WeeklyGoalSheetState,
        updateSheetValues: () -> Unit
    ) {
        sheetViews.dayChecks.forEach { (day, checkBox) ->
            checkBox.setOnCheckedChangeListener { buttonView, isChecked ->
                hapticSelection(buttonView)
                val selectedDays = if (isChecked) {
                    state.reminderSettings.selectedDays + day
                } else {
                    state.reminderSettings.selectedDays - day
                }.ifEmpty { WeeklyGoalPreferences.DEFAULT_REMINDER_DAYS }
                state.reminderSettings = state.reminderSettings.copy(selectedDays = selectedDays)
                updateSheetValues()
            }
        }
        sheetViews.dayRows.forEach { (day, row) ->
            row.setOnClickListener {
                hapticSelection(row)
                sheetViews.dayChecks.getValue(day).isChecked = !sheetViews.dayChecks.getValue(day).isChecked
            }
        }
    }

    private fun wireTargetButtons(
        sheetViews: WeeklyGoalSheetViews,
        state: WeeklyGoalSheetState,
        updateSheetValues: () -> Unit
    ) {
        sheetViews.workoutsDecrease.setOnClickListener { button ->
            hapticSelection(button)
            state.goalSettings = state.goalSettings.copy(
                targetWorkouts = (state.goalSettings.targetWorkouts - 1).coerceAtLeast(0)
            )
            updateSheetValues()
        }
        sheetViews.workoutsIncrease.setOnClickListener { button ->
            hapticSelection(button)
            state.goalSettings = state.goalSettings.copy(
                targetWorkouts = (state.goalSettings.targetWorkouts + 1).coerceAtMost(MAX_TARGET_WORKOUTS)
            )
            updateSheetValues()
        }
        sheetViews.minutesDecrease.setOnClickListener { button ->
            hapticSelection(button)
            state.goalSettings = state.goalSettings.copy(
                targetMinutes = (state.goalSettings.targetMinutes - MINUTES_STEP).coerceAtLeast(0)
            )
            updateSheetValues()
        }
        sheetViews.minutesIncrease.setOnClickListener { button ->
            hapticSelection(button)
            state.goalSettings = state.goalSettings.copy(
                targetMinutes = (state.goalSettings.targetMinutes + MINUTES_STEP).coerceAtMost(MAX_TARGET_MINUTES)
            )
            updateSheetValues()
        }
    }

    private fun wireReminderTimeButtons(
        sheetViews: WeeklyGoalSheetViews,
        state: WeeklyGoalSheetState,
        updateSheetValues: () -> Unit
    ) {
        sheetViews.timeDecrease.setOnClickListener { button ->
            hapticSelection(button)
            state.reminderSettings = adjustReminderTime(state.reminderSettings, -REMINDER_TIME_STEP_MINUTES)
            updateSheetValues()
        }
        sheetViews.timeIncrease.setOnClickListener { button ->
            hapticSelection(button)
            state.reminderSettings = adjustReminderTime(state.reminderSettings, REMINDER_TIME_STEP_MINUTES)
            updateSheetValues()
        }
    }

    private fun adjustReminderTime(
        settings: WeeklyReminderSettings,
        deltaMinutes: Int
    ): WeeklyReminderSettings {
        val minutesPerDay = HOURS_PER_DAY * MINUTES_PER_HOUR
        val currentMinutes = settings.hourOfDay * MINUTES_PER_HOUR + settings.minute
        val nextMinutes = (currentMinutes + deltaMinutes + minutesPerDay) % minutesPerDay
        return settings.copy(
            hourOfDay = nextMinutes / MINUTES_PER_HOUR,
            minute = nextMinutes % MINUTES_PER_HOUR
        )
    }

    private fun wireSheetActions(
        sheetViews: WeeklyGoalSheetViews,
        state: WeeklyGoalSheetState,
        updateSheetValues: () -> Unit,
        bottomSheetDialog: BottomSheetDialog
    ) {
        sheetViews.resetButton.setOnClickListener { button ->
            hapticSelection(button)
            state.goalSettings = WeeklyGoalSettings()
            state.reminderSettings = WeeklyReminderSettings()
            updateSheetValues()
        }
        sheetViews.saveButton.setOnClickListener { button ->
            hapticSuccess(button)
            WeeklyGoalPreferences.saveGoalSettings(sharedPreferences, state.goalSettings)
            WeeklyGoalPreferences.saveReminderSettings(sharedPreferences, state.reminderSettings)
            val remindersCanBeUsed = state.goalSettings.remindersAvailable() && state.reminderSettings.enabled
            val requestedNotificationPermission = requestPostNotificationsForRemindersIfNeeded(remindersCanBeUsed)
            if (!requestedNotificationPermission) {
                requestExactAlarmAccessForRemindersIfNeeded(remindersCanBeUsed)
            }
            activity.lifecycleScope.launch {
                WeeklyReminderScheduler(activity).scheduleNextReminder()
            }
            onSaved()
            bottomSheetDialog.dismiss()
        }
    }

    private fun updateSheetValues(
        sheetViews: WeeklyGoalSheetViews,
        state: WeeklyGoalSheetState
    ) {
        sheetViews.goalSwitch.isChecked = state.goalSettings.enabled
        sheetViews.workoutsValue.text = state.goalSettings.targetWorkouts.toString()
        sheetViews.minutesValue.text = state.goalSettings.targetMinutes.toString()
        sheetViews.reminderSwitch.isChecked = state.reminderSettings.enabled
        sheetViews.timeValue.text = formatReminderTime(
            state.reminderSettings.hourOfDay,
            state.reminderSettings.minute
        )
        sheetViews.pauseSwitch.isChecked = state.reminderSettings.pauseWhenGoalMet
        sheetViews.dayChecks.forEach { (day, checkBox) ->
            checkBox.isChecked = day in state.reminderSettings.selectedDays
        }
        updateWeeklyGoalTargetControlsEnabled(
            targetWorkoutsRow = sheetViews.targetWorkoutsRow,
            targetMinutesRow = sheetViews.targetMinutesRow,
            workoutsDecrease = sheetViews.workoutsDecrease,
            workoutsIncrease = sheetViews.workoutsIncrease,
            minutesDecrease = sheetViews.minutesDecrease,
            minutesIncrease = sheetViews.minutesIncrease,
            enabled = state.goalSettings.enabled
        )
        updateWeeklyGoalReminderControlsEnabled(
            reminderEnabledRow = sheetViews.reminderEnabledRow,
            reminderPauseRow = sheetViews.reminderPauseRow,
            reminderTimeRow = sheetViews.reminderTimeRow,
            reminderDaysLabel = sheetViews.reminderDaysLabel,
            reminderDaysContainer = sheetViews.reminderDaysContainer,
            reminderSwitch = sheetViews.reminderSwitch,
            pauseSwitch = sheetViews.pauseSwitch,
            timeDecrease = sheetViews.timeDecrease,
            timeIncrease = sheetViews.timeIncrease,
            dayChecks = sheetViews.dayChecks.values,
            dayRows = sheetViews.dayRows.values,
            enabled = state.goalSettings.remindersAvailable()
        )
    }

    private fun configureBottomSheet(dialog: BottomSheetDialog, contentView: View) {
        val behavior = dialog.behavior
        behavior.isFitToContents = true
        behavior.isDraggable = true
        behavior.skipCollapsed = false
        behavior.state = BottomSheetBehavior.STATE_COLLAPSED
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<FrameLayout>(
                com.google.android.material.R.id.design_bottom_sheet
            )
            bottomSheet?.layoutParams?.width = ViewGroup.LayoutParams.MATCH_PARENT
            bottomSheet?.requestLayout()
        }

        contentView.doOnLayout {
            val screenHeight = activity.resources.displayMetrics.heightPixels
            val availableWidth = (contentView.parent as? View)?.width?.takeIf { it > 0 }
                ?: activity.resources.displayMetrics.widthPixels
            val minPeekHeight = (screenHeight * MIN_PEEK_HEIGHT_FRACTION).toInt()
            val maxPeekHeight = (screenHeight * MAX_PEEK_HEIGHT_FRACTION).toInt()
            val widthSpec = View.MeasureSpec.makeMeasureSpec(availableWidth, View.MeasureSpec.EXACTLY)
            val heightSpec = View.MeasureSpec.makeMeasureSpec(screenHeight, View.MeasureSpec.AT_MOST)
            contentView.measure(widthSpec, heightSpec)
            behavior.peekHeight = contentView.measuredHeight.coerceIn(minPeekHeight, maxPeekHeight)
        }
    }

    private fun formatReminderTime(hourOfDay: Int, minute: Int): String {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, minute)
        }
        return SimpleDateFormat("h:mm a", Locale.US).format(calendar.time)
    }

    private fun requestPostNotificationsForRemindersIfNeeded(remindersCanBeUsed: Boolean): Boolean {
        val shouldRequest = remindersCanBeUsed &&
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        if (shouldRequest) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                REQUEST_CODE_POST_NOTIFICATIONS
            )
        }
        return shouldRequest
    }

    private fun requestExactAlarmAccessForRemindersIfNeeded(remindersCanBeUsed: Boolean) {
        val shouldRequest = remindersCanBeUsed &&
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S &&
            !WeeklyReminderScheduler(activity).canScheduleExactReminders()
        if (shouldRequest) {
            openExactAlarmSettings()
            Toast.makeText(
                activity,
                R.string.toast_allow_exact_reminders,
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun openExactAlarmSettings() {
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = Uri.parse("package:${activity.packageName}")
        }
        runCatching {
            activity.startActivity(intent)
        }.onFailure {
            activity.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${activity.packageName}")
                }
            )
        }
    }

    private fun hapticSelection(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
    }

    private fun hapticSuccess(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
    }

    private data class WeeklyGoalSheetState(
        var goalSettings: WeeklyGoalSettings,
        var reminderSettings: WeeklyReminderSettings
    )

    private data class WeeklyGoalSheetViews(
        val goalSwitch: SwitchMaterial,
        val targetWorkoutsRow: View,
        val targetMinutesRow: View,
        val workoutsValue: TextView,
        val minutesValue: TextView,
        val reminderEnabledRow: View,
        val reminderSwitch: SwitchMaterial,
        val reminderPauseRow: View,
        val timeValue: TextView,
        val pauseSwitch: SwitchMaterial,
        val reminderTimeRow: View,
        val reminderDaysLabel: TextView,
        val reminderDaysContainer: View,
        val dayChecks: Map<Int, MaterialCheckBox>,
        val dayRows: Map<Int, View>,
        val workoutsDecrease: ImageButton,
        val workoutsIncrease: ImageButton,
        val minutesDecrease: ImageButton,
        val minutesIncrease: ImageButton,
        val timeDecrease: ImageButton,
        val timeIncrease: ImageButton,
        val resetButton: MaterialButton,
        val saveButton: MaterialButton
    ) {
        companion object {
            fun from(view: View): WeeklyGoalSheetViews {
                return WeeklyGoalSheetViews(
                    goalSwitch = view.findViewById(R.id.weeklyGoalEnabledSwitch),
                    targetWorkoutsRow = view.findViewById(R.id.weeklyGoalTargetWorkoutsRow),
                    targetMinutesRow = view.findViewById(R.id.weeklyGoalTargetMinutesRow),
                    workoutsValue = view.findViewById(R.id.weeklyGoalWorkoutsValue),
                    minutesValue = view.findViewById(R.id.weeklyGoalMinutesValue),
                    reminderEnabledRow = view.findViewById(R.id.weeklyReminderEnabledRow),
                    reminderSwitch = view.findViewById(R.id.weeklyReminderEnabledSwitch),
                    reminderPauseRow = view.findViewById(R.id.weeklyReminderPauseRow),
                    timeValue = view.findViewById(R.id.weeklyReminderTimeValue),
                    pauseSwitch = view.findViewById(R.id.weeklyReminderPauseSwitch),
                    reminderTimeRow = view.findViewById(R.id.weeklyReminderTimeRow),
                    reminderDaysLabel = view.findViewById(R.id.weeklyReminderDaysLabel),
                    reminderDaysContainer = view.findViewById(R.id.weeklyReminderDaysContainer),
                    dayChecks = reminderDayCheckboxes(view),
                    dayRows = reminderDayRows(view),
                    workoutsDecrease = view.findViewById(R.id.weeklyGoalWorkoutsDecrease),
                    workoutsIncrease = view.findViewById(R.id.weeklyGoalWorkoutsIncrease),
                    minutesDecrease = view.findViewById(R.id.weeklyGoalMinutesDecrease),
                    minutesIncrease = view.findViewById(R.id.weeklyGoalMinutesIncrease),
                    timeDecrease = view.findViewById(R.id.weeklyReminderTimeDecrease),
                    timeIncrease = view.findViewById(R.id.weeklyReminderTimeIncrease),
                    resetButton = view.findViewById(R.id.weeklyGoalResetButton),
                    saveButton = view.findViewById(R.id.weeklyGoalSaveButton)
                )
            }

            private fun reminderDayCheckboxes(view: View): Map<Int, MaterialCheckBox> {
                return mapOf(
                    Calendar.SUNDAY to view.findViewById(R.id.reminderSunday),
                    Calendar.MONDAY to view.findViewById(R.id.reminderMonday),
                    Calendar.TUESDAY to view.findViewById(R.id.reminderTuesday),
                    Calendar.WEDNESDAY to view.findViewById(R.id.reminderWednesday),
                    Calendar.THURSDAY to view.findViewById(R.id.reminderThursday),
                    Calendar.FRIDAY to view.findViewById(R.id.reminderFriday),
                    Calendar.SATURDAY to view.findViewById(R.id.reminderSaturday)
                )
            }

            private fun reminderDayRows(view: View): Map<Int, View> {
                return mapOf(
                    Calendar.SUNDAY to view.findViewById(R.id.reminderSundayRow),
                    Calendar.MONDAY to view.findViewById(R.id.reminderMondayRow),
                    Calendar.TUESDAY to view.findViewById(R.id.reminderTuesdayRow),
                    Calendar.WEDNESDAY to view.findViewById(R.id.reminderWednesdayRow),
                    Calendar.THURSDAY to view.findViewById(R.id.reminderThursdayRow),
                    Calendar.FRIDAY to view.findViewById(R.id.reminderFridayRow),
                    Calendar.SATURDAY to view.findViewById(R.id.reminderSaturdayRow)
                )
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_POST_NOTIFICATIONS = 1002
        private const val MAX_TARGET_WORKOUTS = 14
        private const val MAX_TARGET_MINUTES = 1000
        private const val MINUTES_STEP = 5
        private const val HOURS_PER_DAY = 24
        private const val MINUTES_PER_HOUR = 60
        private const val REMINDER_TIME_STEP_MINUTES = 30
        private const val SWITCH_TRACK_ALPHA = 0.5f
        private const val MIN_PEEK_HEIGHT_FRACTION = 0.40f
        private const val MAX_PEEK_HEIGHT_FRACTION = 0.80f
    }
}
