package com.oceanofmaya.intervalwalktrainer.home

data class HomeInsightSelectionState(
    val hasUserSelection: Boolean,
    val showEmptyNote: Boolean
)

object HomeInsightSelection {
    fun resolveSelectionState(enabledCardIds: List<String>): HomeInsightSelectionState {
        val hasUserSelection = enabledCardIds.isNotEmpty()
        return HomeInsightSelectionState(
            hasUserSelection = hasUserSelection,
            showEmptyNote = !hasUserSelection
        )
    }

    fun resolveSelectedCards(
        allCards: List<HomeInsightCard>,
        enabledCardIds: List<String>
    ): List<HomeInsightCard> {
        if (enabledCardIds.isEmpty()) {
            return emptyList()
        }
        val cardsById = allCards.associateBy { it.id }
        return enabledCardIds.mapNotNull { cardsById[it] }
    }
}
