package com.oceanofmaya.intervalwalktrainer.home

/**
 * Pure helpers for reordering insight cards within the picker.
 *
 * Moves operate on the full card order (including disabled cards) but only ever swap an
 * enabled card with its nearest enabled neighbour. Keeping disabled cards in their stored
 * positions means re-enabling a card restores the position the user previously chose.
 */
object InsightCardOrdering {
    fun moveUp(order: List<String>, enabledIds: Set<String>, id: String): List<String> {
        return move(order, enabledIds, id, up = true)
    }

    fun moveDown(order: List<String>, enabledIds: Set<String>, id: String): List<String> {
        return move(order, enabledIds, id, up = false)
    }

    private fun move(
        order: List<String>,
        enabledIds: Set<String>,
        id: String,
        up: Boolean
    ): List<String> {
        val index = order.indexOf(id)
        if (index < 0 || id !in enabledIds) return order
        val searchRange = if (up) {
            (index - 1) downTo 0
        } else {
            (index + 1)..order.lastIndex
        }
        val neighbourIndex = searchRange.firstOrNull { order[it] in enabledIds }
        return if (neighbourIndex == null) {
            order
        } else {
            order.toMutableList().apply {
                this[index] = order[neighbourIndex]
                this[neighbourIndex] = id
            }
        }
    }
}
