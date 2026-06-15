package com.oceanofmaya.intervalwalktrainer.home

import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.doOnLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
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

        val recyclerView = view.findViewById<RecyclerView>(R.id.insightCardRecyclerView)
        val helperText = view.findViewById<TextView>(R.id.insightCardsHelperText)

        val registryCards = registry.all()
        val registryIds = registryCards.map { it.id }
        val cardsById = registryCards.associateBy { it.id }

        val enabledIds = HomeInsightPreferences.loadEnabledCardIds(sharedPreferences).toMutableSet()
        var orderIds = HomeInsightPreferences.resolveCardOrder(
            registryOrderedIds = registryIds,
            storedOrder = HomeInsightPreferences.loadCardOrderIds(sharedPreferences)
        )

        val adapter = InsightCardPickerAdapter(
            checkboxTint = checkboxTint(accentColorProvider())
        )
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = adapter

        fun buildRows(): List<InsightCardPickerRow> {
            val enabledOrdered = orderIds.filter { it in enabledIds }
            val disabledOrdered = registryIds.filter { it !in enabledIds }
            val canEnableMore = HomeInsightPreferences.canEnableMoreCard(enabledIds.size)
            val rows = mutableListOf<InsightCardPickerRow>()
            enabledOrdered.forEachIndexed { index, id ->
                val card = cardsById[id] ?: return@forEachIndexed
                rows += InsightCardPickerRow(
                    id = id,
                    title = activity.getString(card.settingsLabelResId),
                    description = card.settingsDescriptionResId?.let { activity.getString(it) },
                    isEnabled = true,
                    canToggle = true,
                    canMoveUp = index > 0,
                    canMoveDown = index < enabledOrdered.lastIndex
                )
            }
            disabledOrdered.forEach { id ->
                val card = cardsById[id] ?: return@forEach
                rows += InsightCardPickerRow(
                    id = id,
                    title = activity.getString(card.settingsLabelResId),
                    description = card.settingsDescriptionResId?.let { activity.getString(it) },
                    isEnabled = false,
                    canToggle = canEnableMore,
                    canMoveUp = false,
                    canMoveDown = false
                )
            }
            return rows
        }

        fun refresh() {
            adapter.submitRows(buildRows())
            helperText.text = HomeInsightPickerCopy.helperText(
                context = activity,
                availableCardCount = registryIds.size,
                enabledCardCount = enabledIds.size
            )
        }

        fun persist() {
            HomeInsightPreferences.saveEnabledCardIds(
                sharedPreferences,
                orderIds.filter { it in enabledIds }
            )
            HomeInsightPreferences.saveCardOrderIds(sharedPreferences, orderIds)
            onSaved()
        }

        adapter.setHandlers(
            onToggle = { id, enabled ->
                if (enabled) {
                    if (id in enabledIds) return@setHandlers
                    if (!HomeInsightPreferences.canEnableMoreCard(enabledIds.size)) {
                        refresh()
                        return@setHandlers
                    }
                    enabledIds += id
                } else {
                    enabledIds -= id
                }
                persist()
                refresh()
            },
            onMoveUp = { id ->
                orderIds = InsightCardOrdering.moveUp(orderIds, enabledIds, id)
                persist()
                refresh()
            },
            onMoveDown = { id ->
                orderIds = InsightCardOrdering.moveDown(orderIds, enabledIds, id)
                persist()
                refresh()
            }
        )

        refresh()
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
    }
}
