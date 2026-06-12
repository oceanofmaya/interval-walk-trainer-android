package com.oceanofmaya.intervalwalktrainer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog

object FaqBottomSheet {
    fun show(
        activity: AppCompatActivity,
        @StringRes scrollToSectionTitleResId: Int? = null
    ) {
        val bottomSheetDialog = BottomSheetDialog(activity)
        val view = LayoutInflater.from(activity)
            .inflate(R.layout.bottom_sheet_faq, FrameLayout(activity), false)
        view.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        bottomSheetDialog.setContentView(view)
        bottomSheetDialog.setOnShowListener {
            val bottomSheet = bottomSheetDialog.findViewById<FrameLayout>(
                com.google.android.material.R.id.design_bottom_sheet
            )
            bottomSheet?.layoutParams?.width = ViewGroup.LayoutParams.MATCH_PARENT
            bottomSheet?.requestLayout()
        }

        bottomSheetDialog.window?.let { window ->
            WindowCompat.setDecorFitsSystemWindows(window, false)
            val recyclerView = view.findViewById<RecyclerView>(R.id.faqRecyclerView)
            val basePaddingBottom = recyclerView.paddingBottom
            ViewCompat.setOnApplyWindowInsetsListener(view) { _, windowInsets ->
                val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
                recyclerView.updatePadding(bottom = basePaddingBottom + insets.bottom)
                windowInsets
            }
        }

        val recyclerView = view.findViewById<RecyclerView>(R.id.faqRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(activity)
        recyclerView.adapter = FaqAdapter(FaqContent.items)
        recyclerView.setHasFixedSize(false)
        scrollToSectionTitleResId?.let { sectionTitleResId ->
            scrollToSection(recyclerView, sectionTitleResId)
        }

        BottomSheetConfigurator.configure(bottomSheetDialog, view)
        bottomSheetDialog.show()
    }

    private fun scrollToSection(
        recyclerView: RecyclerView,
        @StringRes sectionTitleResId: Int
    ) {
        val sectionIndex = FaqContent.items.indexOfFirst { item ->
            item is FaqListItem.Section && item.titleResId == sectionTitleResId
        }
        if (sectionIndex < 0) return
        recyclerView.post {
            (recyclerView.layoutManager as? LinearLayoutManager)
                ?.scrollToPositionWithOffset(sectionIndex, 0)
        }
    }
}
