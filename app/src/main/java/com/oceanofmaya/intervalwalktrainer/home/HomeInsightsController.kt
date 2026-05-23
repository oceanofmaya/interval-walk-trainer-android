package com.oceanofmaya.intervalwalktrainer.home

import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.oceanofmaya.intervalwalktrainer.R
import com.oceanofmaya.intervalwalktrainer.databinding.HomeSectionInsightsBinding
import kotlinx.coroutines.launch

class HomeInsightsController(
    private val activity: AppCompatActivity,
    private val binding: HomeSectionInsightsBinding,
    registry: HomeInsightRegistry
) {
    private val registryCards = registry.all()
    private val adapter = HomeInsightsAdapter()
    private var tabLayoutMediator: TabLayoutMediator? = null
    private var eligibleCards: List<HomeInsightCard> = emptyList()

    init {
        binding.homeInsightsPager.adapter = adapter
        binding.homeInsightsPager.offscreenPageLimit = 1
    }

    fun load() {
        activity.lifecycleScope.launch {
            eligibleCards = registryCards.filter { card ->
                runCatching { card.isEligible() }.getOrDefault(false)
            }
            if (eligibleCards.isEmpty()) {
                binding.homeInsightsSection.visibility = View.GONE
                return@launch
            }

            binding.homeInsightsSection.visibility = View.VISIBLE
            adapter.submitCards(eligibleCards)
            configurePagerBehavior(eligibleCards.size)
        }
    }

    fun applyAccentColor() {
        if (eligibleCards.isNotEmpty()) {
            load()
        }
    }

    private fun configurePagerBehavior(cardCount: Int) {
        val pager = binding.homeInsightsPager
        val indicator = binding.homeInsightsPageIndicator
        val multiCard = cardCount > 1

        pager.isUserInputEnabled = multiCard
        if (multiCard) {
            val peekPx = activity.resources.getDimensionPixelOffset(R.dimen.home_insight_page_peek)
            pager.setPadding(peekPx, 0, peekPx, 0)
            pager.clipToPadding = false
            (pager.getChildAt(0) as? RecyclerView)?.overScrollMode = View.OVER_SCROLL_NEVER
        } else {
            pager.setPadding(0, 0, 0, 0)
            pager.clipToPadding = true
        }

        tabLayoutMediator?.detach()
        tabLayoutMediator = null

        if (multiCard) {
            indicator.visibility = View.VISIBLE
            indicator.removeAllTabs()
            tabLayoutMediator = TabLayoutMediator(indicator, pager) { _, _ -> }.also { it.attach() }
            stylePageIndicator(indicator)
        } else {
            indicator.visibility = View.GONE
            indicator.removeAllTabs()
        }
    }

    private fun stylePageIndicator(tabLayout: TabLayout) {
        for (index in 0 until tabLayout.tabCount) {
            tabLayout.getTabAt(index)?.view?.importantForAccessibility =
                View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
    }
}
