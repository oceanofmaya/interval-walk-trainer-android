package com.oceanofmaya.intervalwalktrainer

import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton

object AccentButtonStyling {
    fun tintFilled(button: MaterialButton, accentColor: Int) {
        val accent = ColorStateList.valueOf(accentColor)
        button.backgroundTintList = accent
        button.setTextColor(ContextCompat.getColor(button.context, R.color.white))
        button.iconTint = ColorStateList.valueOf(
            ContextCompat.getColor(button.context, R.color.white)
        )
    }

    fun tintText(button: MaterialButton, accentColor: Int) {
        val accent = ColorStateList.valueOf(accentColor)
        button.setTextColor(accent)
        button.iconTint = accent
        button.rippleColor = accent
    }
}
