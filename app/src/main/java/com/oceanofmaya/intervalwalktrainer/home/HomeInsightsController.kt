package com.oceanofmaya.intervalwalktrainer.home

import android.content.SharedPreferences
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
    registry: HomeInsightRegistry,
    private val sharedPreferences: SharedPreferences,
    private val onEditInsightCards: () -> Unit
) {
    private val registryCards = registry.all()
    private val adapter = HomeInsightsAdapter()
    private var tabLayoutMediator: TabLayoutMediator? = null
    private var eligibleCards: List<HomeInsightCard> = emptyList()

    init {
        binding.homeInsightsPager.adapter = adapter
        binding.homeInsightsPager.offscreenPageLimit = 1
        binding.homeInsightsEditButton.setOnClickListener { view ->
            view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            onEditInsightCards()
        }
        binding.homeInsightsEmptyCard.setOnClickListener { view ->
            view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            onEditInsightCards()
        }
    }

    fun load() {
        activity.lifecycleScope.launch {
            val enabledCardIds = HomeInsightPreferences.loadEnabledCardIdsOrdered(sharedPreferences)
            val selectionState = HomeInsightSelection.resolveSelectionState(enabledCardIds)
            val selectedCards = HomeInsightSelection.resolveSelectedCards(registryCards, enabledCardIds)

            binding.homeInsightsSection.visibility = View.VISIBLE
            binding.homeInsightsSectionTitle.visibility = View.VISIBLE
            binding.homeInsightsEmptyCard.visibility =
                if (selectionState.showEmptyNote) View.VISIBLE else View.GONE

            if (selectedCards.isEmpty()) {
                eligibleCards = emptyList()
                binding.homeInsightsPager.visibility = View.GONE
                adapter.submitCards(emptyList())
                configurePagerBehavior(0)
                return@launch
            }

            binding.homeInsightsEmptyCard.visibility = View.GONE
            binding.homeInsightsPager.visibility = View.VISIBLE
            eligibleCards = selectedCards.filter { card ->
                runCatching { card.isEligible() }.getOrDefault(false)
            }

            if (eligibleCards.isEmpty()) {
                binding.homeInsightsPager.visibility = View.GONE
                binding.homeInsightsEmptyCard.visibility = View.GONE
                adapter.submitCards(emptyList())
                configurePagerBehavior(0)
                return@launch
            }

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
