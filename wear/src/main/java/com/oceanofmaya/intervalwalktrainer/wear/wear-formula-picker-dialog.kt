package com.oceanofmaya.intervalwalktrainer.wear

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.oceanofmaya.intervalwalktrainer.IntervalFormula
import com.oceanofmaya.intervalwalktrainer.wear.databinding.DialogWearFormulaPickerBinding
import com.oceanofmaya.intervalwalktrainer.wear.databinding.ItemWearFormulaOptionBinding
import kotlin.math.abs

internal class WearFormulaPickerDialog(
    context: Context,
    private val formulas: List<IntervalFormula>,
    private val selectedFormula: IntervalFormula,
    private val displayName: (IntervalFormula) -> String,
    private val onFormulaSelected: (IntervalFormula) -> Unit
) : Dialog(context, R.style.Theme_IntervalWalkTrainer_Wear_FormulaPicker) {
    private lateinit var binding: DialogWearFormulaPickerBinding
    private val snapHelper = LinearSnapHelper()
    private var centeredPosition = formulas.indexOf(selectedFormula)
    private val isCompactScreen =
        context.resources.configuration.screenHeightDp < LARGE_SCREEN_MIN_HEIGHT_DP

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogWearFormulaPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val layoutManager = LinearLayoutManager(context)
        binding.formulaList.apply {
            this.layoutManager = layoutManager
            adapter = FormulaAdapter()
            itemAnimator = null
            setCircularScrollingGestureEnabled(true)
            isVerticalFadingEdgeEnabled = !isCompactScreen
            addOnScrollListener(formulaScrollListener)
        }
        snapHelper.attachToRecyclerView(binding.formulaList)
        binding.formulaList.post {
            val visibleHeight = binding.formulaList.height -
                binding.formulaList.paddingTop -
                binding.formulaList.paddingBottom
            val itemHeight = context.resources.getDimensionPixelSize(
                R.dimen.wear_picker_item_height
            )
            val centerOffset = (visibleHeight - itemHeight) / 2 +
                if (isCompactScreen) binding.formulaList.paddingTop else 0
            layoutManager.scrollToPositionWithOffset(
                centeredPosition.coerceAtLeast(0),
                centerOffset
            )
            updateItemTransforms()
            binding.formulaList.requestFocus()
        }
    }

    private val formulaScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            updateItemTransforms()
        }

        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            if (newState != RecyclerView.SCROLL_STATE_IDLE) return
            val centeredView = snapHelper.findSnapView(recyclerView.layoutManager) ?: return
            val position = recyclerView.getChildAdapterPosition(centeredView)
            if (position != RecyclerView.NO_POSITION && position != centeredPosition) {
                centeredPosition = position
                recyclerView.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            }
        }
    }

    private fun updateItemTransforms() {
        val visibleHeight = binding.formulaList.height -
            binding.formulaList.paddingTop -
            binding.formulaList.paddingBottom
        val listCenter = binding.formulaList.paddingTop + visibleHeight / 2f
        if (listCenter == 0f) return
        repeat(binding.formulaList.childCount) { index ->
            val child = binding.formulaList.getChildAt(index)
            val childCenter = (child.top + child.bottom) / 2f
            val distanceFraction = (abs(listCenter - childCenter) / listCenter).coerceAtMost(1f)
            val scaleReduction = if (isCompactScreen) 0f else EDGE_SCALE_REDUCTION
            val alphaReduction = if (isCompactScreen) {
                COMPACT_EDGE_ALPHA_REDUCTION
            } else {
                EDGE_ALPHA_REDUCTION
            }
            val scale = 1f - distanceFraction * scaleReduction
            child.scaleX = scale
            child.scaleY = scale
            child.alpha = 1f - distanceFraction * alphaReduction
        }
    }

    private inner class FormulaAdapter :
        RecyclerView.Adapter<FormulaAdapter.FormulaViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FormulaViewHolder {
            val itemBinding = ItemWearFormulaOptionBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return FormulaViewHolder(itemBinding)
        }

        override fun onBindViewHolder(holder: FormulaViewHolder, position: Int) {
            holder.bind(formulas[position])
        }

        override fun getItemCount(): Int = formulas.size

        private inner class FormulaViewHolder(
            private val itemBinding: ItemWearFormulaOptionBinding
        ) : RecyclerView.ViewHolder(itemBinding.root) {
            fun bind(formula: IntervalFormula) {
                val isSelected = formula == selectedFormula
                itemBinding.formulaName.text = displayName(formula)
                itemBinding.formulaDuration.text = context.getString(
                    R.string.formula_duration,
                    (formula.totalDurationSeconds / SECONDS_PER_MINUTE).coerceAtLeast(1)
                )
                itemBinding.formulaName.setTextColor(
                    context.getColor(
                        if (isSelected) R.color.wear_accent else R.color.wear_text_primary
                    )
                )
                itemBinding.selectedCheck.isVisible = isSelected
                itemBinding.optionCard.setBackgroundResource(
                    if (isSelected) {
                        R.drawable.wear_formula_selector_selected
                    } else {
                        R.drawable.wear_formula_selector
                    }
                )
                itemBinding.root.contentDescription = context.getString(
                    if (isSelected) R.string.selected_workout else R.string.choose_workout_option,
                    formula.name
                )
                itemBinding.root.setOnClickListener {
                    onFormulaSelected(formula)
                    dismiss()
                }
            }
        }
    }

    private companion object {
        const val SECONDS_PER_MINUTE = 60
        const val LARGE_SCREEN_MIN_HEIGHT_DP = 220
        const val EDGE_SCALE_REDUCTION = 0.16f
        const val EDGE_ALPHA_REDUCTION = 0.52f
        const val COMPACT_EDGE_ALPHA_REDUCTION = 0.36f
    }
}
