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
 * Only one question is expanded at a time.
 */
class FaqAdapter(
    private val items: List<FaqListItem>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private companion object {
        const val VIEW_TYPE_SECTION = 0
        const val VIEW_TYPE_ENTRY = 1
        const val CHEVRON_ROTATION_EXPANDED = 90f
    }

    var expandedPosition = items.indexOfFirst { it is FaqListItem.Entry }.coerceAtLeast(0)
        private set

    class FaqEntryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val header: View = itemView.findViewById(R.id.faqItemHeader)
        val question: TextView = itemView.findViewById(R.id.faqItemQuestion)
        val answer: TextView = itemView.findViewById(R.id.faqItemAnswer)
        val chevron: ImageView = itemView.findViewById(R.id.faqItemChevron)
    }

    class FaqSectionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val topDivider: View = itemView.findViewById(R.id.faqSectionTopDivider)
        val title: TextView = itemView.findViewById(R.id.faqSectionTitle)
        val bottomDivider: View = itemView.findViewById(R.id.faqSectionBottomDivider)
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is FaqListItem.Section -> VIEW_TYPE_SECTION
            is FaqListItem.Entry -> VIEW_TYPE_ENTRY
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_SECTION -> FaqSectionViewHolder(
                inflater.inflate(R.layout.item_faq_section_header, parent, false)
            )
            else -> FaqEntryViewHolder(
                inflater.inflate(R.layout.item_faq, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is FaqListItem.Section -> bindSection(holder as FaqSectionViewHolder, item, position)
            is FaqListItem.Entry -> bindEntry(holder as FaqEntryViewHolder, item, position)
        }
    }

    private fun bindSection(
        holder: FaqSectionViewHolder,
        item: FaqListItem.Section,
        position: Int
    ) {
        holder.title.text = holder.itemView.context.getString(item.titleResId)
        val isFirstSection = position == 0
        holder.topDivider.visibility = if (isFirstSection) View.GONE else View.VISIBLE
        val titleTopPadding = if (isFirstSection) {
            holder.itemView.resources.getDimensionPixelSize(R.dimen.faq_section_header_padding_top_first)
        } else {
            0
        }
        holder.title.setPadding(
            holder.title.paddingLeft,
            titleTopPadding,
            holder.title.paddingRight,
            holder.title.paddingBottom
        )
    }

    private fun bindEntry(
        holder: FaqEntryViewHolder,
        item: FaqListItem.Entry,
        position: Int
    ) {
        val context = holder.itemView.context
        holder.question.text = context.getString(item.entry.questionResId)
        holder.answer.text = context.getString(item.entry.answerResId)

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

    override fun getItemCount(): Int = items.size
}
