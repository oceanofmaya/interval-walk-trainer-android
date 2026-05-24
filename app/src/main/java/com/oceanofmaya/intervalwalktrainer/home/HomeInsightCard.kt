package com.oceanofmaya.intervalwalktrainer.home

import android.view.View
import androidx.annotation.LayoutRes
import androidx.annotation.StringRes

interface HomeInsightCard {
    val id: String

    @get:StringRes
    val settingsLabelResId: Int

    @get:StringRes
    val settingsDescriptionResId: Int?
        get() = null

    @get:LayoutRes
    val layoutResId: Int

    suspend fun isEligible(): Boolean

    fun bind(view: View)

    fun contentDescription(): String
}
