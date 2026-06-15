package com.oceanofmaya.intervalwalktrainer.home

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.checkbox.MaterialCheckBox
import com.oceanofmaya.intervalwalktrainer.R

data class InsightCardPickerRow(
    val id: String,
    val title: String,
    val description: String?,
    val isEnabled: Boolean,
    val canToggle: Boolean,
    val canMoveUp: Boolean,
    val canMoveDown: Boolean
)

class InsightCardPickerAdapter(
    private val checkboxTint: ColorStateList
) : RecyclerView.Adapter<InsightCardPickerAdapter.RowViewHolder>() {

    private var rows: List<InsightCardPickerRow> = emptyList()
    private var onToggle: (id: String, enabled: Boolean) -> Unit = { _, _ -> }
    private var onMoveUp: (id: String) -> Unit = {}
    private var onMoveDown: (id: String) -> Unit = {}

    fun setHandlers(
        onToggle: (id: String, enabled: Boolean) -> Unit,
        onMoveUp: (id: String) -> Unit,
        onMoveDown: (id: String) -> Unit
    ) {
        this.onToggle = onToggle
        this.onMoveUp = onMoveUp
        this.onMoveDown = onMoveDown
    }

    @SuppressLint("NotifyDataSetChanged")
    fun submitRows(newRows: List<InsightCardPickerRow>) {
        rows = newRows
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RowViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(
            R.layout.item_insight_card_picker_row,
            parent,
            false
        )
        return RowViewHolder(view)
    }

    override fun onBindViewHolder(holder: RowViewHolder, position: Int) {
        holder.bind(rows[position])
    }

    override fun getItemCount(): Int = rows.size

    inner class RowViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val label: TextView = itemView.findViewById(R.id.insightCardPickerLabel)
        private val description: TextView = itemView.findViewById(R.id.insightCardPickerDescription)
        private val checkbox: MaterialCheckBox = itemView.findViewById(R.id.insightCardPickerCheckbox)
        private val reorderControls: View = itemView.findViewById(R.id.insightCardPickerReorderControls)
        private val moveUp: ImageButton = itemView.findViewById(R.id.insightCardPickerMoveUp)
        private val moveDown: ImageButton = itemView.findViewById(R.id.insightCardPickerMoveDown)

        fun bind(row: InsightCardPickerRow) {
            val context = itemView.context
            label.text = row.title
            if (row.description != null) {
                description.visibility = View.VISIBLE
                description.text = row.description
            } else {
                description.visibility = View.GONE
            }

            checkbox.setOnCheckedChangeListener(null)
            checkbox.buttonTintList = checkboxTint
            checkbox.isChecked = row.isEnabled
            checkbox.isEnabled = row.isEnabled || row.canToggle
            checkbox.alpha = if (checkbox.isEnabled) 1f else DISABLED_ALPHA
            checkbox.setOnCheckedChangeListener { buttonView, isChecked ->
                buttonView.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                onToggle(row.id, isChecked)
            }

            if (row.isEnabled) {
                reorderControls.visibility = View.VISIBLE
                bindMoveButton(
                    button = moveUp,
                    enabled = row.canMoveUp,
                    descriptionResId = R.string.desc_move_insight_card_up,
                    title = row.title,
                    onClick = { onMoveUp(row.id) }
                )
                bindMoveButton(
                    button = moveDown,
                    enabled = row.canMoveDown,
                    descriptionResId = R.string.desc_move_insight_card_down,
                    title = row.title,
                    onClick = { onMoveDown(row.id) }
                )
            } else {
                reorderControls.visibility = View.GONE
            }
        }

        private fun bindMoveButton(
            button: ImageButton,
            enabled: Boolean,
            descriptionResId: Int,
            title: String,
            onClick: () -> Unit
        ) {
            button.isEnabled = enabled
            button.alpha = if (enabled) 1f else DISABLED_ALPHA
            button.contentDescription = button.context.getString(descriptionResId, title)
            button.importantForAccessibility = if (enabled) {
                View.IMPORTANT_FOR_ACCESSIBILITY_YES
            } else {
                View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
            button.setOnClickListener(
                if (enabled) {
                    View.OnClickListener { view ->
                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        onClick()
                    }
                } else {
                    null
                }
            )
        }
    }

    private companion object {
        const val DISABLED_ALPHA = 0.45f
    }
}
