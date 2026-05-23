package com.oceanofmaya.intervalwalktrainer.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class HomeInsightsAdapter(
    private var cards: List<HomeInsightCard> = emptyList()
) : RecyclerView.Adapter<HomeInsightsAdapter.InsightViewHolder>() {

    fun submitCards(newCards: List<HomeInsightCard>) {
        cards = newCards
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int = cards[position].layoutResId

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InsightViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
        view.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        return InsightViewHolder(view)
    }

    override fun onBindViewHolder(holder: InsightViewHolder, position: Int) {
        cards[position].bind(holder.itemView)
    }

    override fun getItemCount(): Int = cards.size

    class InsightViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
