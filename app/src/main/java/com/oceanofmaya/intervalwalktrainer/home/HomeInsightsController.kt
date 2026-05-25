package com.oceanofmaya.intervalwalktrainer.home

import android.content.SharedPreferences
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.MarginPageTransformer
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
    private val accentColorProvider: () -> Int,
    private val onEditInsightCards: () -> Unit
) {
    private val registryCards = registry.all()
    private val adapter = HomeInsightsAdapter()
    private var tabLayoutMediator: TabLayoutMediator? = null
    private var eligibleCards: List<HomeInsightCard> = emptyList()
    private var singleCardLayoutResId: Int? = null

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
            val previouslyFocusedCardId = focusedCardId()
            val enabledCardIds = HomeInsightPreferences.loadEnabledCardIdsOrdered(sharedPreferences)
            val selectionState = HomeInsightSelection.resolveSelectionState(enabledCardIds)
            val selectedCards = HomeInsightSelection.resolveSelectedCards(registryCards, enabledCardIds)

            binding.homeInsightsSection.visibility = View.VISIBLE
            binding.homeInsightsSectionTitle.visibility = View.VISIBLE
            binding.homeInsightsEmptyCard.visibility =
                if (selectionState.showEmptyNote) View.VISIBLE else View.GONE

            if (selectedCards.isEmpty()) {
                eligibleCards = emptyList()
                hideAllCardHosts()
                adapter.submitCards(emptyList())
                configurePagerBehavior(0)
                return@launch
            }

            binding.homeInsightsEmptyCard.visibility = View.GONE
            eligibleCards = selectedCards.filter { card ->
                runCatching { card.isEligible() }.getOrDefault(false)
            }

            if (eligibleCards.isEmpty()) {
                hideAllCardHosts()
                adapter.submitCards(emptyList())
                configurePagerBehavior(0)
                return@launch
            }

            when (eligibleCards.size) {
                1 -> showSingleCard(eligibleCards.first())
                else -> showPagerCards(eligibleCards, previouslyFocusedCardId)
            }
        }
    }

    fun applyAccentColor() {
        if (eligibleCards.isEmpty()) {
            return
        }
        when (eligibleCards.size) {
            1 -> showSingleCard(eligibleCards.first())
            else -> {
                adapter.notifyDataSetChanged()
                if (binding.homeInsightsPageIndicator.visibility == View.VISIBLE) {
                    stylePageIndicator(binding.homeInsightsPageIndicator)
                }
            }
        }
    }

    private fun hideAllCardHosts() {
        binding.homeInsightsSingleCardHost.visibility = View.GONE
        binding.homeInsightsSingleCardHost.removeAllViews()
        singleCardLayoutResId = null
        binding.homeInsightsPager.visibility = View.GONE
    }

    private fun showSingleCard(card: HomeInsightCard) {
        binding.homeInsightsPager.visibility = View.GONE
        adapter.submitCards(emptyList())
        configurePagerBehavior(1)

        val host = binding.homeInsightsSingleCardHost
        host.visibility = View.VISIBLE

        val cardView = if (singleCardLayoutResId == card.layoutResId && host.childCount == 1) {
            host.getChildAt(0)
        } else {
            host.removeAllViews()
            singleCardLayoutResId = card.layoutResId
            LayoutInflater.from(activity)
                .inflate(card.layoutResId, host, false)
                .also { view ->
                    view.layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                    host.addView(view)
                }
        }

        card.bind(cardView)
    }

    private fun showPagerCards(cards: List<HomeInsightCard>, focusedCardId: String?) {
        binding.homeInsightsSingleCardHost.visibility = View.GONE
        binding.homeInsightsSingleCardHost.removeAllViews()
        singleCardLayoutResId = null
        binding.homeInsightsPager.visibility = View.VISIBLE
        adapter.submitCards(cards)
        configurePagerBehavior(cards.size)
        val focusedIndex = cards.indexOfFirst { it.id == focusedCardId }
            .takeIf { it >= 0 }
            ?: binding.homeInsightsPager.currentItem.coerceIn(0, cards.lastIndex)
        binding.homeInsightsPager.setCurrentItem(focusedIndex, false)
    }

    private fun focusedCardId(): String? {
        if (eligibleCards.size <= 1 || binding.homeInsightsPager.visibility != View.VISIBLE) {
            return eligibleCards.singleOrNull()?.id
        }
        return eligibleCards.getOrNull(binding.homeInsightsPager.currentItem)?.id
    }

    private fun configurePagerBehavior(cardCount: Int) {
        val pager = binding.homeInsightsPager
        val indicator = binding.homeInsightsPageIndicator
        val multiCard = cardCount > 1
        val recyclerView = pager.getChildAt(0) as? RecyclerView

        pager.isUserInputEnabled = multiCard
        pager.setPadding(0, 0, 0, 0)
        pager.clipToPadding = true
        recyclerView?.clipToPadding = true

        if (multiCard) {
            val gapPx = activity.resources.getDimensionPixelOffset(R.dimen.home_insight_page_gap)
            pager.setPageTransformer(MarginPageTransformer(gapPx))
            recyclerView?.overScrollMode = View.OVER_SCROLL_NEVER
        } else {
            pager.setPageTransformer(null)
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
        val dotSize = activity.resources.getDimensionPixelSize(R.dimen.home_insight_page_indicator_dot_size)
        val dotGap = activity.resources.getDimensionPixelSize(R.dimen.home_insight_page_indicator_gap)
        val mutedColor = ContextCompat.getColor(activity, R.color.text_secondary)
        tabLayout.tabMode = TabLayout.MODE_SCROLLABLE

        for (index in 0 until tabLayout.tabCount) {
            val tabView = tabLayout.getTabAt(index)?.view ?: continue
            tabView.apply {
                background = createPageIndicatorDotBackground(
                    accentColor = accentColorProvider(),
                    mutedColor = mutedColor,
                    sizePx = dotSize
                )
                minimumWidth = dotSize
                minimumHeight = dotSize
                updateLayoutParams<ViewGroup.MarginLayoutParams> {
                    width = dotSize
                    height = dotSize
                    marginStart = if (index == 0) 0 else dotGap
                }
                setPadding(0, 0, 0, 0)
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }
        }
    }

    private fun createPageIndicatorDotBackground(
        accentColor: Int,
        mutedColor: Int,
        sizePx: Int
    ): StateListDrawable {
        return StateListDrawable().apply {
            addState(
                intArrayOf(android.R.attr.state_selected),
                createPageIndicatorDot(accentColor, sizePx)
            )
            addState(
                intArrayOf(),
                createPageIndicatorDot(mutedColor, sizePx)
            )
        }
    }

    private fun createPageIndicatorDot(color: Int, sizePx: Int): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(color)
            setSize(sizePx, sizePx)
        }
    }
}
