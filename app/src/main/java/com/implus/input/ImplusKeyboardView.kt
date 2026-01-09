package com.implus.input

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.implus.input.layout.*
import kotlin.math.abs

class ImplusKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var currentPage: KeyboardPage? = null
    private var nextPage: KeyboardPage? = null
    private var slideOffset = 0f // 0f = currentPage fully visible, -1f = sliding left, 1f = sliding right

    private val keyDrawables = mutableListOf<KeyDrawable>()
    private val nextKeyDrawables = mutableListOf<KeyDrawable>()
    
    var activeStates: Map<String, Boolean> = emptyMap()
        set(value) { field = value; invalidate() }
        
    var onKeyListener: ((KeyboardKey) -> Unit)? = null
    var onSwipeListener: ((Direction) -> Unit)? = null

    // Settings
    var swipeThreshold = 50 
    var horizontalSpacing = 6
    var verticalSpacing = 6

    enum class Direction { LEFT, RIGHT }

    private var colorBg = 0; private var colorKey = 0; private var colorFuncKey = 0
    private var colorText = 0; private var colorSticky = 0; private var colorStickyActive = 0
    private var colorStickyTextActive = 0
    private var colorRipple = 0
    
    private val shadowPaint = Paint()
    private val keyPaint = Paint()
    private val textPaint = Paint().apply { textAlign = Paint.Align.CENTER; isAntiAlias = true }
    private val ripplePaint = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
    private val clipPath = Path()

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            if (e1 == null) return false
            val diffX = e2.x - e1.x
            val diffY = e2.y - e1.y
            if (abs(diffX) > abs(diffY) && abs(diffX) > swipeThreshold && abs(velocityX) > 100) {
                if (diffX > 0) onSwipeListener?.invoke(Direction.RIGHT)
                else onSwipeListener?.invoke(Direction.LEFT)
                return true
            }
            return false
        }
    })

    init { applyTheme() }

    fun getCurrentPageId(): String? = currentPage?.id

    private fun applyTheme() {
        val isDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        
        if (isDark) {
            colorBg = context.getColor(R.color.keyboard_bg_dark)
            colorKey = context.getColor(R.color.key_bg_dark)
            colorFuncKey = context.getColor(R.color.func_key_bg_dark)
            colorText = context.getColor(R.color.text_dark)
            colorSticky = context.getColor(R.color.sticky_inactive_dark)
            colorStickyActive = context.getColor(R.color.sticky_active_dark)
            colorStickyTextActive = context.getColor(R.color.sticky_text_active_dark)
            colorRipple = context.getColor(R.color.ripple_dark)
        } else {
            colorBg = context.getColor(R.color.keyboard_bg_light)
            colorKey = context.getColor(R.color.key_bg_light)
            colorFuncKey = context.getColor(R.color.func_key_bg_light)
            colorText = context.getColor(R.color.text_light)
            colorSticky = context.getColor(R.color.sticky_inactive_light)
            colorStickyActive = context.getColor(R.color.sticky_active_light)
            colorStickyTextActive = context.getColor(R.color.sticky_text_active_light)
            colorRipple = context.getColor(R.color.ripple_light)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        applyTheme()
        invalidate()
    }

    fun setPage(page: KeyboardPage, animateDirection: Direction? = null) {
        if (animateDirection == null) {
            this.currentPage = page
            layoutKeys(page, keyDrawables, width, height)
            invalidate()
        } else {
            this.nextPage = page
            layoutKeys(page, nextKeyDrawables, width, height)
            
            val end = if (animateDirection == Direction.LEFT) -1f else 1f
            
            val animator = ValueAnimator.ofFloat(0f, 1f)
            animator.duration = 200
            animator.interpolator = DecelerateInterpolator()
            animator.addUpdateListener { 
                val fraction = it.animatedValue as Float
                slideOffset = end * fraction
                invalidate()
            }
            animator.start()
            
            postDelayed({
                this.currentPage = page
                layoutKeys(page, keyDrawables, width, height)
                slideOffset = 0f
                nextPage = null
                invalidate()
            }, 200)
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        currentPage?.let { layoutKeys(it, keyDrawables, width, height) }
    }

    private fun layoutKeys(page: KeyboardPage, targetList: MutableList<KeyDrawable>, w: Int, h: Int) {
        if (w <= 0 || h <= 0) return
        targetList.clear()
        val rowHeight = h / page.rows.size.toFloat()
        var curY = 0f
        
        val hSpacing = horizontalSpacing.toFloat()
        val vSpacing = verticalSpacing.toFloat()

        for (row in page.rows) {
            val totalWeight = row.keys.sumOf { it.weight.toDouble() }.toFloat()
            var curX = 0f
            for (key in row.keys) {
                val keyW = key.weight * (w / totalWeight)
                
                val rect = RectF(
                    curX + hSpacing / 2f, 
                    curY + vSpacing / 2f, 
                    curX + keyW - hSpacing / 2f, 
                    curY + rowHeight - vSpacing / 2f
                )
                
                var textSize = rowHeight * 0.4f
                val label = key.label ?: ""
                if (label.isNotEmpty()) {
                    textPaint.textSize = textSize
                    val textW = textPaint.measureText(label)
                    val maxTextW = rect.width() * 0.8f
                    if (textW > maxTextW) {
                        textSize *= (maxTextW / textW)
                    }
                }
                
                targetList.add(KeyDrawable(key, rect, textSize))
                curX += keyW
            }
            curY += rowHeight
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(colorBg)

        val w = width.toFloat()
        
        if (slideOffset != 0f && nextPage != null) {
            val currentTransX = slideOffset * w
            
            canvas.save()
            canvas.translate(currentTransX, 0f)
            for (kd in keyDrawables) drawKey(canvas, kd)
            canvas.restore()
            
            val nextStartX = if (slideOffset < 0) w else -w
            canvas.save()
            canvas.translate(nextStartX + currentTransX, 0f)
            for (kd in nextKeyDrawables) drawKey(canvas, kd)
            canvas.restore()
            
        } else {
            for (kd in keyDrawables) drawKey(canvas, kd)
        }
    }

    private fun drawKey(canvas: Canvas, kd: KeyDrawable) {
        val key = kd.key
        val rect = kd.rect
        
        var effectiveLabel = key.label ?: ""
        var effectiveStyle = key.style
        
        key.overrides?.forEach { (stateId, override) ->
            if (activeStates[stateId] == true) {
                override.label?.let { effectiveLabel = it }
                override.style?.let { effectiveStyle = it }
            }
        }

        var paintColor = colorKey
        var radius = 24f 
        var textColor = colorText

        when (effectiveStyle) {
            KeyStyle.FUNCTION -> { paintColor = colorFuncKey; radius = 12f }
            KeyStyle.STICKY -> {
                val isActive = key.id?.let { activeStates[it] } ?: false
                paintColor = if (isActive) colorStickyActive else colorSticky
                if (isActive) {
                    textColor = colorStickyTextActive
                }
                radius = 12f
            }
            else -> { if (key.type != KeyType.NORMAL) paintColor = colorFuncKey }
        }

        shadowPaint.color = Color.argb(30, 0, 0, 0)
        canvas.drawRoundRect(rect.left, rect.top + 3f, rect.right, rect.bottom + 3f, radius, radius, shadowPaint)
        keyPaint.color = paintColor
        canvas.drawRoundRect(rect, radius, radius, keyPaint)
        
        // Ripple Effect
        if (kd.rippleIntensity > 0f) {
            canvas.save()
            clipPath.reset()
            clipPath.addRoundRect(rect, radius, radius, Path.Direction.CW)
            canvas.clipPath(clipPath)
            
            ripplePaint.color = colorRipple
            // Scale the alpha based on intensity
            val baseAlpha = Color.alpha(colorRipple)
            ripplePaint.alpha = (baseAlpha * kd.rippleIntensity).toInt()
            
            canvas.drawCircle(kd.rippleX, kd.rippleY, kd.rippleRadius, ripplePaint)
            canvas.restore()
        }

        textPaint.color = textColor
        textPaint.textSize = kd.baseTextSize 
        
        val baseline = rect.centerY() - (textPaint.fontMetrics.bottom + textPaint.fontMetrics.top) / 2
        canvas.drawText(effectiveLabel, rect.centerX(), baseline, textPaint)
    }

    private var pressedKey: KeyDrawable? = null
    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (gestureDetector.onTouchEvent(e)) return true
        
        val x = e.x; val y = e.y
        when (e.action) {
            MotionEvent.ACTION_DOWN -> { 
                pressedKey = keyDrawables.find { it.rect.contains(x, y) }
                pressedKey?.let { 
                    it.onPressed(x, y)
                    invalidate()
                } 
            }
            MotionEvent.ACTION_UP -> { 
                pressedKey?.let { 
                    if (it.rect.contains(x, y)) onKeyListener?.invoke(it.key)
                    it.onReleased()
                    pressedKey = null
                    invalidate() 
                } 
            }
            MotionEvent.ACTION_CANCEL -> {
                pressedKey?.let { 
                    it.onReleased()
                    pressedKey = null
                    invalidate() 
                }
            }
            MotionEvent.ACTION_MOVE -> { 
                if (pressedKey != null && !pressedKey!!.rect.contains(x, y)) { 
                    pressedKey!!.onReleased()
                    pressedKey = null
                    invalidate() 
                } 
            }
        }
        return true
    }
    
    private inner class KeyDrawable(val key: KeyboardKey, val rect: RectF, val baseTextSize: Float) { 
        // Ripple State
        var rippleX = 0f
        var rippleY = 0f
        var rippleRadius = 0f
        var rippleIntensity = 0f // 0f to 1f
        
        private var radiusAnimator: ValueAnimator? = null
        private var alphaAnimator: ValueAnimator? = null

        fun onPressed(x: Float, y: Float) {
            rippleX = x
            rippleY = y
            rippleRadius = 0f
            rippleIntensity = 1f
            
            // Calculate max radius to cover the key
            val maxRadius = Math.hypot(rect.width().toDouble(), rect.height().toDouble()).toFloat()
            
            radiusAnimator?.cancel()
            radiusAnimator = ValueAnimator.ofFloat(0f, maxRadius).apply {
                duration = 350
                interpolator = DecelerateInterpolator()
                addUpdateListener { 
                    rippleRadius = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
            
            // Should stay at max intensity while pressed, so we don't animate alpha down here.
            alphaAnimator?.cancel()
            alphaAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 50
                addUpdateListener {
                    rippleIntensity = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
        }
        
        fun onReleased() {
            // Fade out
            alphaAnimator?.cancel()
            alphaAnimator = ValueAnimator.ofFloat(rippleIntensity, 0f).apply {
                duration = 200
                addUpdateListener {
                    rippleIntensity = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
        }
    }
}
