package com.oceanofmaya.intervalwalktrainer

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton

object WhatsNewBottomSheet {
    fun show(
        activity: AppCompatActivity,
        accentColor: Int,
        onFinished: () -> Unit
    ) {
        val bottomSheetDialog = BottomSheetDialog(activity)
        val view = LayoutInflater.from(activity)
            .inflate(R.layout.bottom_sheet_whats_new, FrameLayout(activity), false)
        view.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        bottomSheetDialog.setContentView(view)
        bottomSheetDialog.setOnShowListener {
            val bottomSheet = bottomSheetDialog.findViewById<FrameLayout>(
                com.google.android.material.R.id.design_bottom_sheet
            )
            bottomSheet?.layoutParams?.width = ViewGroup.LayoutParams.MATCH_PARENT
            bottomSheet?.requestLayout()
        }

        val learnMoreButton = view.findViewById<MaterialButton>(R.id.whatsNewLearnMoreButton)
        val notNowButton = view.findViewById<MaterialButton>(R.id.whatsNewNotNowButton)
        AccentButtonStyling.tintFilled(learnMoreButton, accentColor)
        AccentButtonStyling.tintText(notNowButton, accentColor)

        fun finish() {
            onFinished()
            bottomSheetDialog.dismiss()
        }

        learnMoreButton.setOnClickListener {
            finish()
            FaqBottomSheet.show(
                activity = activity,
                scrollToSectionTitleResId = R.string.faq_section_health_connect
            )
        }
        notNowButton.setOnClickListener { finish() }
        bottomSheetDialog.setOnCancelListener { onFinished() }

        BottomSheetConfigurator.configure(bottomSheetDialog, view)
        bottomSheetDialog.show()
    }
}
