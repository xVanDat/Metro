/*
 * Copyright (c) 2026 Metro contributors.
 * Licensed under the GNU General Public License v3.
 */
package code.name.monkey.retromusic.views

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import androidx.core.graphics.ColorUtils
import com.google.android.material.color.MaterialColors
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/** Animated background whose palette is derived from the current album cover. */
class AnimatedAlbumGradientView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val lightBackground = ColorUtils.calculateLuminance(
        MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurface),
    ) > 0.5
    private var phase = 0f
    private var animationEnabled = false
    private var palette = intArrayOf(Color.rgb(36, 23, 64), Color.rgb(35, 69, 102), Color.rgb(83, 31, 73))
    private var targetPalette = palette.copyOf()

    private val motionAnimator = ValueAnimator.ofFloat(0f, TWO_PI).apply {
        duration = 16_000L
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            phase = it.animatedValue as Float
            postInvalidateOnAnimation()
        }
    }
    private var paletteAnimator: ValueAnimator? = null

    fun setAlbumColors(primary: Int, secondary: Int) {
        animationEnabled = true
        startMotion()
        val next = createPalette(primary, secondary)
        if (next.contentEquals(targetPalette)) return
        targetPalette = next
        val previous = palette.copyOf()
        paletteAnimator?.cancel()
        paletteAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 650L
            addUpdateListener { animator ->
                val fraction = animator.animatedFraction
                palette = IntArray(next.size) { index ->
                    ColorUtils.blendARGB(previous[index], next[index], fraction)
                }
                invalidate()
            }
            start()
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (animationEnabled) startMotion()
    }

    override fun onDetachedFromWindow() {
        paletteAnimator?.cancel()
        motionAnimator.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!animationEnabled || width == 0 || height == 0) return

        val diagonal = max(width, height).toFloat()
        val xShift = cos(phase.toDouble()).toFloat() * width * 0.28f
        val yShift = sin(phase.toDouble()).toFloat() * height * 0.22f
        paint.shader = LinearGradient(
            -xShift, -yShift, width + xShift, height + yShift,
            intArrayOf(palette[0], palette[1], palette[2], palette[0]),
            floatArrayOf(0f, 0.34f, 0.72f, 1f), Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        repeat(3) { index ->
            val offset = phase + index * (TWO_PI / 3f)
            val cx = width * (0.5f + 0.42f * cos(offset.toDouble()).toFloat())
            val cy = height * (0.5f + 0.36f * sin((offset * 0.83f).toDouble()).toFloat())
            val glow = ColorUtils.setAlphaComponent(palette[(index + 1) % palette.size], 150)
            paint.shader = RadialGradient(
                cx, cy, diagonal * 0.78f, glow, Color.TRANSPARENT, Shader.TileMode.CLAMP,
            )
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        }
        paint.shader = null
    }

    private fun startMotion() {
        if (isAttachedToWindow && !motionAnimator.isStarted) motionAnimator.start()
    }

    private fun createPalette(primary: Int, secondary: Int): IntArray {
        val secondSource = if (secondary == Color.TRANSPARENT) primary else secondary
        val lightness = if (lightBackground) floatArrayOf(0.82f, 0.72f, 0.86f) else floatArrayOf(0.30f, 0.38f, 0.26f)
        return intArrayOf(
            harmonize(primary, 0f, 0.64f, lightness[0]),
            harmonize(secondSource, 28f, 0.70f, lightness[1]),
            harmonize(primary, -42f, 0.72f, lightness[2]),
        )
    }

    private fun harmonize(color: Int, hueOffset: Float, minSaturation: Float, lightness: Float): Int {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(color, hsl)
        hsl[0] = (hsl[0] + hueOffset + 360f) % 360f
        hsl[1] = max(hsl[1], minSaturation)
        hsl[2] = lightness
        return ColorUtils.HSLToColor(hsl)
    }

    private companion object {
        const val TWO_PI = 6.2831855f
    }
}
