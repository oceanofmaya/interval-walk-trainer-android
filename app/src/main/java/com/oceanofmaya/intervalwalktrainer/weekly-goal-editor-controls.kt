package com.oceanofmaya.intervalwalktrainer

import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.switchmaterial.SwitchMaterial

private const val DISABLED_CONTROL_ALPHA = 0.4f

internal fun updateWeeklyGoalTargetControlsEnabled(
    targetWorkoutsRow: View,
    targetMinutesRow: View,
    workoutsDecrease: ImageButton,
    workoutsIncrease: ImageButton,
    minutesDecrease: ImageButton,
    minutesIncrease: ImageButton,
    enabled: Boolean
) {
    val alpha = if (enabled) 1f else DISABLED_CONTROL_ALPHA
    listOf(targetWorkoutsRow, targetMinutesRow).forEach { row ->
        row.alpha = alpha
    }
    listOf(workoutsDecrease, workoutsIncrease, minutesDecrease, minutesIncrease).forEach { button ->
        button.isEnabled = enabled
    }
}

internal fun updateWeeklyGoalReminderControlsEnabled(
    reminderEnabledRow: View,
    reminderPauseRow: View,
    reminderTimeRow: View,
    reminderDaysLabel: TextView,
    reminderDaysContainer: View,
    reminderSwitch: SwitchMaterial,
    pauseSwitch: SwitchMaterial,
    timeDecrease: ImageButton,
    timeIncrease: ImageButton,
    dayChecks: Collection<MaterialCheckBox>,
    dayRows: Collection<View>,
    enabled: Boolean
) {
    val alpha = if (enabled) 1f else DISABLED_CONTROL_ALPHA
    listOf(
        reminderEnabledRow,
        reminderPauseRow,
        reminderTimeRow,
        reminderDaysLabel,
        reminderDaysContainer
    ).forEach { view ->
        view.alpha = alpha
    }
    listOf(reminderSwitch, pauseSwitch).forEach { switch ->
        switch.isEnabled = enabled
    }
    listOf(timeDecrease, timeIncrease).forEach { button ->
        button.isEnabled = enabled
    }
    dayChecks.forEach { checkBox ->
        checkBox.isEnabled = enabled
    }
    dayRows.forEach { row ->
        row.isEnabled = enabled
    }
}
