package com.oceanofmaya.intervalwalktrainer.home

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.oceanofmaya.intervalwalktrainer.R
import com.oceanofmaya.intervalwalktrainer.WeeklyGoalUiFormatter

object HomeInsightCircleBinder {
    fun bindStatusCircle(
        statusRoot: View,
        iconResId: Int,
        iconTintColor: Int,
        accessibilityLabel: String,
        formatter: WeeklyGoalUiFormatter
    ) {
        val iconView = statusRoot.findViewById<ImageView>(R.id.weeklyGoalStatusCircleIcon)
        iconView.setImageResource(iconResId)
        iconView.imageTintList = android.content.res.ColorStateList.valueOf(iconTintColor)
        formatter.styleStatusCircle(
            fillView = statusRoot.findViewById(R.id.weeklyGoalStatusCircleFill)
        )
        statusRoot.contentDescription = accessibilityLabel
    }

    fun bindFlatStatCircle(
        statRoot: View,
        valueText: String,
        captionText: String,
        valueTextColor: Int,
        captionTextColor: Int,
        formatter: HomeInsightUiFormatter
    ) {
        statRoot.visibility = View.VISIBLE
        statRoot.findViewById<TextView>(R.id.homeInsightFlatStatValue).apply {
            text = valueText
            setTextColor(valueTextColor)
        }
        statRoot.findViewById<TextView>(R.id.homeInsightFlatStatCaption).apply {
            text = captionText
            setTextColor(captionTextColor)
        }
        formatter.styleFlatStatCircle(
            fillView = statRoot.findViewById(R.id.homeInsightFlatStatCircleFill)
        )
        statRoot.contentDescription = "$valueText $captionText"
    }

    fun hideFlatStatCircle(statRoot: View) {
        statRoot.visibility = View.GONE
    }
}
