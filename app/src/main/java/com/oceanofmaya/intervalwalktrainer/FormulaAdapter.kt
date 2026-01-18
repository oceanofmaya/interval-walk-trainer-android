package com.oceanofmaya.intervalwalktrainer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * RecyclerView adapter for displaying interval training formulas in a list.
 * Used in the bottom sheet formula selector dialog.
 * 
 * @param formulas List of formulas to display
 * @param showCustomOption Whether to show a "Custom" option at the end
 * @param onItemClick Callback invoked when a formula is selected (null for custom option)
 */
class FormulaAdapter(
    private val formulas: List<IntervalFormula>,
    private val showCustomOption: Boolean = false,
    private val onItemClick: (IntervalFormula?) -> Unit
) : RecyclerView.Adapter<FormulaAdapter.FormulaViewHolder>() {
    
    companion object {
        internal const val VIEW_TYPE_FORMULA = 0
        internal const val VIEW_TYPE_CUSTOM = 1
    }
    
    /**
     * ViewHolder for formula items.
     * 
     * @param itemView The root view of the item layout
     */
    class FormulaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val formulaName: TextView = itemView.findViewById(R.id.formulaName)
    }
    
    override fun getItemViewType(position: Int): Int {
        return if (showCustomOption && position == formulas.size) {
            VIEW_TYPE_CUSTOM
        } else {
            VIEW_TYPE_FORMULA
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FormulaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_formula, parent, false)
        return FormulaViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: FormulaViewHolder, position: Int) {
        if (showCustomOption && position == formulas.size) {
            // Custom option
            holder.formulaName.text = holder.itemView.context.getString(R.string.custom_formula)
            holder.itemView.setOnClickListener {
                onItemClick(null)
            }
        } else {
            // Regular formula
            val formula = formulas[position]
            holder.formulaName.text = formula.name
            holder.itemView.setOnClickListener {
                onItemClick(formula)
            }
        }
    }
    
    override fun getItemCount() = formulas.size + if (showCustomOption) 1 else 0
}

