package com.oceanofmaya.intervalwalktrainer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.doOnLayout

class CoachMarkOverlay(context: Context) : FrameLayout(context) {
    private val scrimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xB3000000.toInt()
    }
    private val clearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }
    private val highlightRect = RectF()
    private val highlightRadius = dp(6).toFloat()

    init {
        isClickable = true
        isFocusable = true
        setWillNotDraw(false)
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    fun show(anchor: View, title: String, message: String, onDismiss: () -> Unit, paddingDp: Int = 4) {
        val bubble = createBubble(title, message)
        addView(
            bubble,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        )

        setOnClickListener {
            (parent as? ViewGroup)?.removeView(this)
            onDismiss()
        }

        doOnLayout {
            updateHighlight(anchor, paddingDp)
            positionBubble(bubble)
            invalidate()
        }
    }

    override fun dispatchDraw(canvas: Canvas) {
        val save = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), scrimPaint)
        canvas.drawRoundRect(highlightRect, highlightRadius, highlightRadius, clearPaint)
        canvas.restoreToCount(save)
        super.dispatchDraw(canvas)
    }

    private fun updateHighlight(anchor: View, paddingDp: Int) {
        val anchorRect = Rect()
        val rootRect = Rect()
        anchor.getGlobalVisibleRect(anchorRect)
        getGlobalVisibleRect(rootRect)
        val padding = dp(paddingDp).toFloat()
        highlightRect.set(
            anchorRect.left - rootRect.left - padding,
            anchorRect.top - rootRect.top - padding,
            anchorRect.right - rootRect.left + padding,
            anchorRect.bottom - rootRect.top + padding
        )
    }

    private fun positionBubble(bubble: View) {
        val spacing = dp(12)
        val horizontalPadding = dp(16)
        val maxWidth = (width * 0.8f).toInt().coerceAtLeast(dp(200))

        bubble.measure(
            MeasureSpec.makeMeasureSpec(maxWidth, MeasureSpec.AT_MOST),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        )

        val bubbleWidth = bubble.measuredWidth
        val bubbleHeight = bubble.measuredHeight
        val showAbove = highlightRect.centerY() > height * 0.6f
        val rawTop = if (showAbove) {
            highlightRect.top - bubbleHeight - spacing
        } else {
            highlightRect.bottom + spacing
        }

        val left = (highlightRect.centerX() - bubbleWidth / 2f).coerceIn(
            horizontalPadding.toFloat(),
            (width - bubbleWidth - horizontalPadding).toFloat()
        )
        val top = rawTop.coerceIn(
            horizontalPadding.toFloat(),
            (height - bubbleHeight - horizontalPadding).toFloat()
        )

        val params = bubble.layoutParams as LayoutParams
        params.leftMargin = left.toInt()
        params.topMargin = top.toInt()
        bubble.layoutParams = params
    }

    private fun createBubble(title: String, message: String): LinearLayout {
        val background = GradientDrawable().apply {
            cornerRadius = dp(8).toFloat()
            setColor(ContextCompat.getColor(context, R.color.surface))
            setStroke(dp(2), ContextCompat.getColor(context, R.color.button_primary))
        }
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            this.background = background
            elevation = dp(10).toFloat()

            val titleView = TextView(context).apply {
                text = title
                textSize = 16f
                setTextColor(ContextCompat.getColor(context, R.color.text_primary))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
                textAlignment = TEXT_ALIGNMENT_CENTER
                isAllCaps = true
                letterSpacing = 0.04f
            }

            val messageView = TextView(context).apply {
                text = message
                textSize = 13f
                setTextColor(ContextCompat.getColor(context, R.color.text_secondary))
            }

            addView(titleView)
            addView(messageView)
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    companion object {
        fun show(
            root: ViewGroup,
            anchor: View,
            title: String,
            message: String,
            onDismiss: () -> Unit,
            paddingDp: Int = 4
        ) {
            val overlay = CoachMarkOverlay(root.context)
            root.addView(
                overlay,
                LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT
                )
            )
            overlay.show(anchor, title, message, onDismiss, paddingDp)
        }
    }
}
