package com.oceanofmaya.intervalwalktrainer.home

import android.content.SharedPreferences
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.StateListDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeInsightsController(
    private val activity: AppCompatActivity,
    private val binding: HomeSectionInsightsBinding,
    registry: HomeInsightRegistry,
    private val sharedPreferences: SharedPreferences,
    private val accentColorProvider: () -> Int,
    private val onEditInsightCards: () -> Unit
) {
    private val registryCards = registry.all()
    private var pagerAdapter = HomeInsightsAdapter()
    private var tabLayoutMediator: TabLayoutMediator? = null
    private var eligibleCards: List<HomeInsightCard> = emptyList()
    private var loadRequestId: Int = 0

    private val pageChangeCallback = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            binding.homeInsightsPager.post { rebindVisibleInsightCards() }
        }
    }

    init {
        binding.homeInsightsPager.adapter = pagerAdapter
        binding.homeInsightsPager.offscreenPageLimit = 1
        binding.homeInsightsPager.registerOnPageChangeCallback(pageChangeCallback)
        binding.homeInsightsEditButton.setOnClickListener { view ->
            view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            onEditInsightCards()
        }
        binding.homeInsightsEmptyCard.setOnClickListener { view ->
            view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)
            onEditInsightCards()
        }
    }

    fun load(onComplete: (() -> Unit)? = null) {
        val requestId = ++loadRequestId
        activity.lifecycleScope.launch {
            val previouslyFocusedCardId = focusedCardId()
            val enabledCardIds = HomeInsightPreferences.loadEnabledCardIdsOrdered(sharedPreferences)
            val selectionState = HomeInsightSelection.resolveSelectionState(enabledCardIds)
            val selectedCards = HomeInsightSelection.resolveSelectedCards(registryCards, enabledCardIds)

            ensureSectionChromeVisible(selectionState)

            if (selectedCards.isEmpty()) {
                if (requestId != loadRequestId) return@launch
                eligibleCards = emptyList()
                hideAllCardHosts()
                resetPagerAdapter(emptyList())
                configurePagerBehavior(0)
                onComplete?.invoke()
                return@launch
            }

            binding.homeInsightsEmptyCard.visibility = View.GONE

            val resolvedCards = withContext(Dispatchers.IO) {
                selectedCards.filter { card ->
                    runCatching { card.isEligible() }.getOrDefault(true)
                }
            }

            if (requestId != loadRequestId) return@launch

            eligibleCards = resolvedCards
            if (eligibleCards.isEmpty()) {
                hideAllCardHosts()
                resetPagerAdapter(emptyList())
                configurePagerBehavior(0)
                onComplete?.invoke()
                return@launch
            }

            if (requestId != loadRequestId) return@launch

            when (eligibleCards.size) {
                1 -> showSingleCard(eligibleCards.first())
                else -> showPagerCards(eligibleCards, previouslyFocusedCardId)
            }
            scheduleInsightRebindAfterLayout(onComplete)
        }
    }

    fun applyAccentColor() {
        if (eligibleCards.isEmpty()) {
            return
        }
        when (eligibleCards.size) {
            1 -> showSingleCard(eligibleCards.first())
            else -> {
                rebindVisibleInsightCards()
                if (binding.homeInsightsPageIndicator.visibility == View.VISIBLE) {
                    binding.homeInsightsPageIndicator.post {
                        stylePageIndicator(binding.homeInsightsPageIndicator)
                    }
                }
                scheduleInsightRebindAfterLayout()
            }
        }
    }

    private fun ensureSectionChromeVisible(selectionState: HomeInsightSelectionState) {
        binding.homeInsightsSection.visibility = View.VISIBLE
        binding.homeInsightsSectionTitle.visibility = View.VISIBLE
        binding.homeInsightsSectionTitle.setTextColor(
            ContextCompat.getColor(activity, R.color.text_primary)
        )
        binding.homeInsightsEmptyCard.visibility =
            if (selectionState.showEmptyNote) View.VISIBLE else View.GONE
    }

    private fun hideAllCardHosts() {
        binding.homeInsightsSingleCardHost.visibility = View.GONE
        binding.homeInsightsSingleCardHost.removeAllViews()
        binding.homeInsightsPager.visibility = View.GONE
    }

    private fun showSingleCard(card: HomeInsightCard) {
        binding.homeInsightsPager.visibility = View.GONE
        resetPagerAdapter(emptyList())
        configurePagerBehavior(1)

        val host = binding.homeInsightsSingleCardHost
        host.visibility = View.VISIBLE
        host.removeAllViews()

        val cardView = LayoutInflater.from(activity)
            .inflate(card.layoutResId, host, false)
            .also { view ->
                view.layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                host.addView(view)
            }

        card.bind(cardView)
        ensureSectionChromeVisible(
            HomeInsightSelection.resolveSelectionState(
                HomeInsightPreferences.loadEnabledCardIdsOrdered(sharedPreferences)
            )
        )
    }

    private fun showPagerCards(cards: List<HomeInsightCard>, focusedCardId: String?) {
        binding.homeInsightsSingleCardHost.visibility = View.GONE
        binding.homeInsightsSingleCardHost.removeAllViews()
        binding.homeInsightsPager.visibility = View.VISIBLE
        resetPagerAdapter(cards)
        configurePagerBehavior(cards.size)
        val focusedIndex = cards.indexOfFirst { it.id == focusedCardId }
            .takeIf { it >= 0 }
            ?: binding.homeInsightsPager.currentItem.coerceIn(0, cards.lastIndex)
        binding.homeInsightsPager.setCurrentItem(focusedIndex, false)
        ensureSectionChromeVisible(
            HomeInsightSelection.resolveSelectionState(
                HomeInsightPreferences.loadEnabledCardIdsOrdered(sharedPreferences)
            )
        )
    }

    private fun resetPagerAdapter(cards: List<HomeInsightCard>) {
        tabLayoutMediator?.detach()
        tabLayoutMediator = null
        pagerAdapter = HomeInsightsAdapter()
        pagerAdapter.submitCards(cards)
        binding.homeInsightsPager.adapter = pagerAdapter
    }

    private fun scheduleInsightRebindAfterLayout(onComplete: (() -> Unit)? = null) {
        val pager = binding.homeInsightsPager
        val host = binding.homeInsightsSingleCardHost
        val target = if (pager.visibility == View.VISIBLE) pager else host

        target.post {
            if (target.width > 0) {
                rebindVisibleInsightCards()
                onComplete?.invoke()
                return@post
            }

            val observer = target.viewTreeObserver
            observer.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    if (target.width <= 0) {
                        return
                    }
                    target.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    rebindVisibleInsightCards()
                    onComplete?.invoke()
                }
            })
        }
    }

    private fun rebindVisibleInsightCards() {
        if (eligibleCards.size <= 1) {
            val host = binding.homeInsightsSingleCardHost
            if (host.visibility == View.VISIBLE && host.childCount == 1) {
                eligibleCards.singleOrNull()?.bind(host.getChildAt(0))
            }
            return
        }

        val pager = binding.homeInsightsPager
        val recyclerView = pager.getChildAt(0) as? RecyclerView ?: return
        val reboundPositions = mutableSetOf<Int>()

        for (index in 0 until recyclerView.childCount) {
            val child = recyclerView.getChildAt(index)
            val position = recyclerView.getChildAdapterPosition(child)
            if (position != RecyclerView.NO_POSITION && position < eligibleCards.size) {
                eligibleCards[position].bind(child)
                reboundPositions += position
            }
        }

        val currentPosition = pager.currentItem
        if (currentPosition in eligibleCards.indices && currentPosition !in reboundPositions) {
            recyclerView.findViewHolderForAdapterPosition(currentPosition)?.itemView?.let { view ->
                eligibleCards[currentPosition].bind(view)
            }
        }
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

        if (multiCard) {
            indicator.visibility = View.VISIBLE
            indicator.removeAllTabs()
            tabLayoutMediator = TabLayoutMediator(indicator, pager) { _, _ -> }.also { it.attach() }
            indicator.post { stylePageIndicator(indicator) }
        } else {
            tabLayoutMediator?.detach()
            tabLayoutMediator = null
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
