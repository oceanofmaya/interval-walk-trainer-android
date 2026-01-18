package com.oceanofmaya.intervalwalktrainer

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class ConfettiView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private data class Particle(
        var x: Float,
        var y: Float,
        val velocityX: Float,
        val velocityY: Float,
        val size: Float,
        val color: Int,
        val rotationSpeed: Float,
        var rotation: Float
    )

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val particles = mutableListOf<Particle>()
    private val random = Random(System.currentTimeMillis())
    private var animator: ValueAnimator? = null
    private var lastFrameTime = 0L

    fun launch() {
        if (visibility != View.VISIBLE) {
            visibility = View.VISIBLE
            alpha = 1f
        }

        if (width == 0 || height == 0) {
            post { launch() }
            return
        }

        particles.clear()
        val colors = intArrayOf(
            0xFF2196F3.toInt(),
            0xFFE53935.toInt(),
            0xFF43A047.toInt(),
            0xFFFFA000.toInt(),
            0xFF8E24AA.toInt()
        )

        val count = 120
        for (i in 0 until count) {
            val startX = random.nextFloat() * width
            val startY = -random.nextFloat() * (height * 0.2f)
            val velocityX = (random.nextFloat() - 0.5f) * 220f
            val velocityY = 360f + random.nextFloat() * 520f
            val size = 8f + random.nextFloat() * 14f
            val color = colors[random.nextInt(colors.size)]
            val rotationSpeed = (random.nextFloat() - 0.5f) * 360f
            particles.add(
                Particle(
                    x = startX,
                    y = startY,
                    velocityX = velocityX,
                    velocityY = velocityY,
                    size = size,
                    color = color,
                    rotationSpeed = rotationSpeed,
                    rotation = random.nextFloat() * 360f
                )
            )
        }

        bringToFront()
        animator?.cancel()
        lastFrameTime = 0L
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1700L
            addUpdateListener {
                val now = System.currentTimeMillis()
                val dt = if (lastFrameTime == 0L) 0f else (now - lastFrameTime) / 1000f
                lastFrameTime = now
                updateParticles(dt)
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    visibility = View.GONE
                }
            })
            start()
        }
    }

    private fun updateParticles(deltaSeconds: Float) {
        if (deltaSeconds <= 0f) return
        val gravity = 620f
        particles.forEach { particle ->
            particle.x += particle.velocityX * deltaSeconds
            particle.y += particle.velocityY * deltaSeconds
            particle.rotation += particle.rotationSpeed * deltaSeconds
            particle.y += 0.5f * gravity * deltaSeconds * deltaSeconds
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (particles.isEmpty()) return

        val maxX = width.toFloat()
        val maxY = height.toFloat()
        particles.forEach { particle ->
            if (particle.x + particle.size < 0f || particle.x - particle.size > maxX) return@forEach
            if (particle.y - particle.size > maxY) return@forEach

            paint.color = particle.color
            canvas.save()
            canvas.translate(particle.x, particle.y)
            canvas.rotate(particle.rotation)
            val half = particle.size / 2f
            canvas.drawRect(-half, -half, half, half, paint)
            canvas.restore()
        }
    }
}
