package com.oceanofmaya.intervalwalktrainer

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

/**
 * Formula bottom sheet: preset section + saved presets (drag reorder + long-press / overflow actions).
 *
 * "Design Your Own" lives outside this adapter as a sticky footer button so it stays reachable
 * regardless of how many saved presets the user has.
 */
class FormulaSheetAdapter(
    private val context: Context,
    savedWorkouts: List<SavedWorkout>,
    private val onPickPreset: (IntervalFormula) -> Unit,
    private val onPickSaved: (SavedWorkout) -> Unit,
    private val onEmptyCreate: () -> Unit,
    private val onSavedLongPress: (SavedWorkout, View) -> Unit,
    private val onOrderChanged: (List<Long>) -> Unit,
    private val onSavedAction: (SavedWorkoutAction, SavedWorkout) -> Unit = { _, _ -> }
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    enum class SavedWorkoutAction { EDIT, RENAME, DUPLICATE, DELETE }

    private val saved = savedWorkouts.toMutableList()
    private val items = mutableListOf<Row>()

    private sealed class Row {
        data object HeaderPresets : Row()
        data class Preset(val formula: IntervalFormula) : Row()
        data class HeaderSaved(val count: Int, val atCap: Boolean) : Row()
        data class SavedRow(val workout: SavedWorkout) : Row()
        data object EmptySaved : Row()
    }

    init {
        rebuildItems()
    }

    fun updateSaved(newSaved: List<SavedWorkout>) {
        // Skip rebinds when nothing visible has changed (id+order). Prevents the
        // Room invalidation-tracker self-emit (after persistOrder/insert) from
        // triggering another notifyDataSetChanged that would glitch an in-flight drag.
        if (sameVisibleOrder(newSaved, saved)) return
        saved.clear()
        saved.addAll(newSaved)
        rebuildItems()
        notifyDataSetChanged()
    }

    private fun sameVisibleOrder(incoming: List<SavedWorkout>, current: List<SavedWorkout>): Boolean {
        if (incoming.size != current.size) return false
        return incoming.zip(current).all { (a, b) -> sameVisibleRow(a, b) }
    }

    private fun sameVisibleRow(a: SavedWorkout, b: SavedWorkout): Boolean =
        a.id == b.id && a.displayName == b.displayName && a.sortOrder == b.sortOrder

    private fun rebuildItems() {
        items.clear()
        items.add(Row.HeaderPresets)
        IntervalFormulas.all.forEach { items.add(Row.Preset(it)) }
        val atCap = saved.size >= SavedWorkoutRepository.MAX_SAVED_WORKOUTS
        items.add(Row.HeaderSaved(saved.size, atCap))
        if (saved.isEmpty()) {
            items.add(Row.EmptySaved)
        } else {
            saved.forEach { items.add(Row.SavedRow(it)) }
        }
    }

    fun isSavedViewHolder(holder: RecyclerView.ViewHolder): Boolean =
        holder.bindingAdapterPosition != RecyclerView.NO_POSITION &&
            items.getOrNull(holder.bindingAdapterPosition) is Row.SavedRow

    private fun adapterPositionToSavedIndex(adapterPos: Int): Int? = when {
        adapterPos !in items.indices -> null
        items[adapterPos] !is Row.SavedRow -> null
        else -> items.take(adapterPos).count { it is Row.SavedRow }
    }

    /**
     * Called by ItemTouchHelper.onMove for every adjacent swap during a drag.
     * Updates in-memory order with notifyItemMoved (preserves drag animation) but does
     * NOT persist - that happens once on drop in [onRowDropped] to avoid a feedback loop
     * where each persist re-emits the Flow and rebinds rows mid-gesture.
     */
    fun onRowMoved(fromAdapterPos: Int, toAdapterPos: Int): Boolean {
        val fromIdx = adapterPositionToSavedIndex(fromAdapterPos)
        val toIdx = adapterPositionToSavedIndex(toAdapterPos)
        return when {
            fromIdx == null || toIdx == null -> false
            fromIdx == toIdx -> true
            else -> {
                val moved = saved.removeAt(fromIdx)
                saved.add(toIdx, moved)
                rebuildItems()
                notifyItemMoved(fromAdapterPos, toAdapterPos)
                pendingOrderDirty = true
                true
            }
        }
    }

    /** Called from ItemTouchHelper.clearView (drag finished) to persist exactly once. */
    fun onRowDropped() {
        if (pendingOrderDirty) {
            pendingOrderDirty = false
            onOrderChanged(saved.map { it.id })
        }
    }

    private var pendingOrderDirty = false

    companion object {
        private const val VT_HEADER = 0
        private const val VT_PRESET = 1
        private const val VT_SAVED = 2
        private const val VT_EMPTY = 3

        private const val ACTION_ID_BASE = 0x00100000
        private const val ACTION_ID_EDIT = ACTION_ID_BASE + 1
        private const val ACTION_ID_RENAME = ACTION_ID_BASE + 2
        private const val ACTION_ID_DUPLICATE = ACTION_ID_BASE + 3
        private const val ACTION_ID_DELETE = ACTION_ID_BASE + 4

        private const val SECONDS_PER_MINUTE = 60

        /**
         * Shared subtitle formatter for both pre-configured and saved preset rows.
         * Renders e.g. "30 min · Interval" or "28 min · Circuit".
         */
        private fun subtitleFor(context: Context, formula: IntervalFormula): String {
            val mins = formula.totalDurationSeconds / SECONDS_PER_MINUTE
            val type = if (formula.isCircuit) {
                context.getString(R.string.label_workout_type_circuit)
            } else {
                context.getString(R.string.label_workout_type_interval)
            }
            return context.getString(R.string.format_saved_preset_subtitle, mins, type)
        }
    }

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is Row.HeaderPresets, is Row.HeaderSaved -> VT_HEADER
        is Row.Preset -> VT_PRESET
        is Row.SavedRow -> VT_SAVED
        is Row.EmptySaved -> VT_EMPTY
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VT_HEADER -> HeaderVH(
                inflater.inflate(R.layout.item_formula_section_header, parent, false)
            )
            VT_PRESET -> PresetVH(
                inflater.inflate(R.layout.item_formula_preset, parent, false)
            )
            VT_SAVED -> SavedVH(
                inflater.inflate(R.layout.item_saved_workout_row, parent, false)
            )
            VT_EMPTY -> EmptyVH(
                inflater.inflate(R.layout.item_formula_empty_saved, parent, false)
            )
            else -> error("unknown type")
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = items[position]) {
            is Row.HeaderPresets -> {
                (holder as HeaderVH).title.setText(R.string.header_preconfigured_presets)
                holder.subtitle.visibility = View.GONE
            }
            is Row.HeaderSaved -> {
                (holder as HeaderVH).title.text = if (row.atCap) {
                    context.getString(
                        R.string.header_my_saved_presets_cap,
                        row.count,
                        SavedWorkoutRepository.MAX_SAVED_WORKOUTS
                    )
                } else {
                    context.getString(R.string.header_my_saved_presets, row.count)
                }
                if (row.count > 0) {
                    holder.subtitle.setText(R.string.hint_saved_presets_section)
                    holder.subtitle.visibility = View.VISIBLE
                } else {
                    holder.subtitle.visibility = View.GONE
                }
            }
            is Row.Preset -> {
                (holder as PresetVH).bind(row.formula)
                holder.itemView.setOnClickListener { onPickPreset(row.formula) }
            }
            is Row.SavedRow -> {
                (holder as SavedVH).bind(row.workout)
                holder.itemView.setOnClickListener { onPickSaved(row.workout) }
                holder.itemView.setOnLongClickListener {
                    onSavedLongPress(row.workout, holder.moreButton)
                    true
                }
                holder.moreButton.setOnClickListener {
                    onSavedLongPress(row.workout, holder.moreButton)
                }
                attachSavedAccessibilityActions(holder.itemView, row.workout)
                holder.dragHandle.setOnTouchListener { v, e ->
                    if (e.actionMasked == MotionEvent.ACTION_DOWN) {
                        holder.itemView.parent?.let { p ->
                            (p as? RecyclerView)?.let { rv ->
                                rv.findContainingViewHolder(v)?.let { vh ->
                                    touchHelperRef?.startDrag(vh)
                                }
                            }
                        }
                    }
                    false
                }
            }
            is Row.EmptySaved -> {
                (holder as EmptyVH).bind(onEmptyCreate)
            }
        }
    }

    private fun attachSavedAccessibilityActions(itemView: View, workout: SavedWorkout) {
        ViewCompat.setAccessibilityDelegate(itemView, object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfoCompat) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.addAction(
                    AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                        ACTION_ID_EDIT,
                        context.getString(R.string.action_edit)
                    )
                )
                info.addAction(
                    AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                        ACTION_ID_RENAME,
                        context.getString(R.string.action_rename)
                    )
                )
                info.addAction(
                    AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                        ACTION_ID_DUPLICATE,
                        context.getString(R.string.action_duplicate)
                    )
                )
                info.addAction(
                    AccessibilityNodeInfoCompat.AccessibilityActionCompat(
                        ACTION_ID_DELETE,
                        context.getString(R.string.action_delete)
                    )
                )
            }

            override fun performAccessibilityAction(host: View, action: Int, args: android.os.Bundle?): Boolean {
                return when (action) {
                    ACTION_ID_EDIT -> {
                        onSavedAction(SavedWorkoutAction.EDIT, workout); true
                    }
                    ACTION_ID_RENAME -> {
                        onSavedAction(SavedWorkoutAction.RENAME, workout); true
                    }
                    ACTION_ID_DUPLICATE -> {
                        onSavedAction(SavedWorkoutAction.DUPLICATE, workout); true
                    }
                    ACTION_ID_DELETE -> {
                        onSavedAction(SavedWorkoutAction.DELETE, workout); true
                    }
                    else -> super.performAccessibilityAction(host, action, args)
                }
            }
        })
    }

    private var touchHelperRef: ItemTouchHelper? = null

    fun attachItemTouchHelper(recyclerView: RecyclerView) {
        val callback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN,
            0
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return onRowMoved(viewHolder.bindingAdapterPosition, target.bindingAdapterPosition)
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) = Unit

            override fun isLongPressDragEnabled(): Boolean = false

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                onRowDropped()
            }

            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                return if (isSavedViewHolder(viewHolder)) {
                    makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0)
                } else {
                    makeMovementFlags(0, 0)
                }
            }

            override fun canDropOver(
                recyclerView: RecyclerView,
                current: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = isSavedViewHolder(target)
        }
        val helper = ItemTouchHelper(callback)
        helper.attachToRecyclerView(recyclerView)
        touchHelperRef = helper
    }

    private class HeaderVH(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.sectionTitle)
        val subtitle: TextView = view.findViewById(R.id.sectionSubtitle)
    }

    private class PresetVH(view: View) : RecyclerView.ViewHolder(view) {
        private val name: TextView = view.findViewById(R.id.formulaName)
        private val subtitle: TextView = view.findViewById(R.id.formulaSubtitle)

        fun bind(formula: IntervalFormula) {
            name.text = formula.name
            subtitle.text = subtitleFor(itemView.context, formula)
        }
    }

    private class SavedVH(view: View) : RecyclerView.ViewHolder(view) {
        val dragHandle: View = view.findViewById(R.id.dragHandle)
        val moreButton: View = view.findViewById(R.id.savedWorkoutMoreButton)
        private val title: TextView = view.findViewById(R.id.savedWorkoutTitle)
        private val subtitle: TextView = view.findViewById(R.id.savedWorkoutSubtitle)

        fun bind(workout: SavedWorkout) {
            title.text = workout.displayName
            subtitle.text = subtitleFor(itemView.context, workout.toIntervalFormula())
        }
    }

    private class EmptyVH(view: View) : RecyclerView.ViewHolder(view) {
        private val title: TextView = view.findViewById(R.id.emptySavedTitle)
        private val hint: TextView = view.findViewById(R.id.emptySavedHint)
        private val btn: MaterialButton = view.findViewById(R.id.emptySavedCreateButton)

        fun bind(onCreate: () -> Unit) {
            title.setText(R.string.body_no_saved_presets_yet_title)
            hint.setText(R.string.body_no_saved_presets_yet_hint)
            btn.setText(R.string.action_custom_formula)
            btn.setOnClickListener { onCreate() }
        }
    }
}
