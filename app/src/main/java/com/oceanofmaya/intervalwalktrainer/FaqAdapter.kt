package com.oceanofmaya.intervalwalktrainer

import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * Data for a single FAQ entry (question and answer string resource IDs).
 */
data class FaqEntry(
    val questionResId: Int,
    val answerResId: Int
)

/**
 * RecyclerView adapter for the FAQ accordion in the Help bottom sheet.
 * Only one item is expanded at a time.
 *
 * @param entries List of FAQ question/answer pairs
 */
class FaqAdapter(
    private val entries: List<FaqEntry>
) : RecyclerView.Adapter<FaqAdapter.FaqViewHolder>() {

    private companion object {
        const val CHEVRON_ROTATION_EXPANDED = 90f
    }

    var expandedPosition = 0
        private set

    class FaqViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val header: View = itemView.findViewById(R.id.faqItemHeader)
        val question: TextView = itemView.findViewById(R.id.faqItemQuestion)
        val answer: TextView = itemView.findViewById(R.id.faqItemAnswer)
        val chevron: ImageView = itemView.findViewById(R.id.faqItemChevron)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FaqViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_faq, parent, false)
        return FaqViewHolder(view)
    }

    override fun onBindViewHolder(holder: FaqViewHolder, position: Int) {
        val entry = entries[position]
        val context = holder.itemView.context
        holder.question.text = context.getString(entry.questionResId)
        holder.answer.text = context.getString(entry.answerResId)

        val isExpanded = position == expandedPosition
        holder.answer.visibility = if (isExpanded) View.VISIBLE else View.GONE
        holder.chevron.rotation = if (isExpanded) CHEVRON_ROTATION_EXPANDED else 0f

        holder.header.setOnClickListener {
            val clickedPosition = holder.bindingAdapterPosition
            if (clickedPosition == RecyclerView.NO_POSITION) return@setOnClickListener
            if (expandedPosition == clickedPosition) return@setOnClickListener
            val previous = expandedPosition
            expandedPosition = clickedPosition
            notifyItemChanged(previous)
            notifyItemChanged(clickedPosition)
            holder.header.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }

    override fun getItemCount(): Int = entries.size
}
