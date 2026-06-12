package com.oceanofmaya.intervalwalktrainer.home

import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.checkbox.MaterialCheckBox
import com.oceanofmaya.intervalwalktrainer.R

class InsightCardsEditor(
    private val activity: AppCompatActivity,
    private val sharedPreferences: SharedPreferences,
    private val registry: HomeInsightRegistry,
    private val accentColorProvider: () -> Int,
    private val onSaved: () -> Unit
) {
    fun show() {
        val bottomSheetDialog = BottomSheetDialog(activity)
        val view = LayoutInflater.from(activity).inflate(
            R.layout.bottom_sheet_insight_cards,
            FrameLayout(activity),
            false
        )
        view.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        bottomSheetDialog.setContentView(view)
        configureBottomSheet(bottomSheetDialog, view)

        val rowsContainer = view.findViewById<LinearLayout>(R.id.insightCardRows)
        val helperText = view.findViewById<TextView>(R.id.insightCardsHelperText)
        val enabledIds = HomeInsightPreferences.loadEnabledCardIdsOrdered(sharedPreferences).toMutableList()
        val checkboxTint = checkboxTint(accentColorProvider())
        val rowCheckboxes = mutableListOf<MaterialCheckBox>()

        fun pickerOrderedEnabledIds(): List<String> {
            val enabledIdSet = enabledIds.toSet()
            return registry.all().map { it.id }.filter { it in enabledIdSet }
        }

        fun refreshRowStates() {
            val atLimit = !HomeInsightPreferences.canEnableMoreCard(enabledIds.size)
            rowCheckboxes.forEach { checkbox ->
                if (!checkbox.isChecked) {
                    checkbox.isEnabled = !atLimit
                    checkbox.alpha = if (atLimit) DISABLED_ROW_ALPHA else 1f
                } else {
                    checkbox.isEnabled = true
                    checkbox.alpha = 1f
                }
            }
            helperText.text = HomeInsightPickerCopy.helperText(
                context = activity,
                availableCardCount = registry.all().size,
                enabledCardCount = enabledIds.size
            )
        }

        registry.all().forEach { card ->
            val row = LayoutInflater.from(activity).inflate(
                R.layout.item_insight_card_picker_row,
                rowsContainer,
                false
            )
            val label = row.findViewById<TextView>(R.id.insightCardPickerLabel)
            val description = row.findViewById<TextView>(R.id.insightCardPickerDescription)
            val checkbox = row.findViewById<MaterialCheckBox>(R.id.insightCardPickerCheckbox)

            label.text = activity.getString(card.settingsLabelResId)
            val descriptionResId = card.settingsDescriptionResId
            if (descriptionResId != null) {
                description.visibility = View.VISIBLE
                description.text = activity.getString(descriptionResId)
            } else {
                description.visibility = View.GONE
            }

            checkbox.buttonTintList = checkboxTint
            checkbox.isChecked = card.id in enabledIds
            rowCheckboxes += checkbox

            lateinit var listener: CompoundButton.OnCheckedChangeListener
            listener = CompoundButton.OnCheckedChangeListener { buttonView, isChecked ->
                buttonView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                if (isChecked) {
                    if (card.id in enabledIds) {
                        return@OnCheckedChangeListener
                    }
                    if (!HomeInsightPreferences.canEnableMoreCard(enabledIds.size)) {
                        checkbox.setOnCheckedChangeListener(null)
                        checkbox.isChecked = false
                        checkbox.setOnCheckedChangeListener(listener)
                        return@OnCheckedChangeListener
                    }
                    enabledIds += card.id
                } else {
                    enabledIds.remove(card.id)
                }
                val orderedEnabledIds = pickerOrderedEnabledIds()
                enabledIds.clear()
                enabledIds += orderedEnabledIds
                HomeInsightPreferences.saveEnabledCardIds(sharedPreferences, enabledIds)
                onSaved()
                refreshRowStates()
            }
            checkbox.setOnCheckedChangeListener(listener)

            rowsContainer.addView(row)
        }

        refreshRowStates()
        bottomSheetDialog.show()
    }

    private fun checkboxTint(accentColor: Int): ColorStateList {
        return ColorStateList(
            arrayOf(
                intArrayOf(android.R.attr.state_checked),
                intArrayOf(-android.R.attr.state_checked)
            ),
            intArrayOf(
                accentColor,
                activity.getColor(R.color.text_secondary)
            )
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

    companion object {
        private const val MIN_PEEK_HEIGHT_FRACTION = 0.45f
        private const val MAX_PEEK_HEIGHT_FRACTION = 0.92f
        private const val DISABLED_ROW_ALPHA = 0.45f
    }
}
