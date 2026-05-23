package com.oceanofmaya.intervalwalktrainer.home

import android.view.View
import androidx.annotation.LayoutRes

interface HomeInsightCard {
    @get:LayoutRes
    val layoutResId: Int

    suspend fun isEligible(): Boolean

    fun bind(view: View)

    fun contentDescription(): String
}
