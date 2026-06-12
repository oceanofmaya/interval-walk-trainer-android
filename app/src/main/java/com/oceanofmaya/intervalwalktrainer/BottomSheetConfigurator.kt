package com.oceanofmaya.intervalwalktrainer

import android.view.View
import androidx.core.view.doOnLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

object BottomSheetConfigurator {
    private const val MIN_PEEK_HEIGHT_RATIO = 0.40f
    private const val MAX_PEEK_HEIGHT_RATIO = 0.80f

    fun configure(dialog: BottomSheetDialog, contentView: View) {
        val behavior = dialog.behavior
        behavior.isFitToContents = true
        behavior.isDraggable = true
        behavior.skipCollapsed = false
        behavior.state = BottomSheetBehavior.STATE_COLLAPSED

        contentView.doOnLayout {
            val resources = contentView.resources
            val screenHeight = resources.displayMetrics.heightPixels
            val availableWidth = (contentView.parent as? View)?.width?.takeIf { it > 0 }
                ?: resources.displayMetrics.widthPixels
            val minPeekHeight = (screenHeight * MIN_PEEK_HEIGHT_RATIO).toInt()
            val maxPeekHeight = (screenHeight * MAX_PEEK_HEIGHT_RATIO).toInt()
            val widthSpec = View.MeasureSpec.makeMeasureSpec(availableWidth, View.MeasureSpec.EXACTLY)
            val heightSpec = View.MeasureSpec.makeMeasureSpec(screenHeight, View.MeasureSpec.AT_MOST)
            contentView.measure(widthSpec, heightSpec)
            val contentHeight = contentView.measuredHeight
            val finalPeekHeight = if (contentHeight <= maxPeekHeight) {
                contentHeight
            } else {
                maxPeekHeight
            }
            behavior.peekHeight = finalPeekHeight.coerceAtLeast(minPeekHeight)
        }
    }
}
