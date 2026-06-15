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
        enabledCardIds: List<String>,
        orderedCardIds: List<String>? = null
    ): List<HomeInsightCard> {
        if (enabledCardIds.isEmpty()) {
            return emptyList()
        }
        val enabledIdSet = enabledCardIds.toSet()
        val cardsById = allCards.associateBy { it.id }
        val resolvedOrder = HomeInsightPreferences.resolveCardOrder(
            registryOrderedIds = allCards.map { it.id },
            storedOrder = orderedCardIds
        )
        return resolvedOrder
            .filter { it in enabledIdSet }
            .mapNotNull { cardsById[it] }
    }
}
