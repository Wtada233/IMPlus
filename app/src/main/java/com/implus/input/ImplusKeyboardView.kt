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
import com.implus.input.manager.AssetResourceManager
import kotlin.math.abs

class ImplusKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val assetRes by lazy { AssetResourceManager(context) }

    var layoutThemeLight: KeyboardTheme? = null
    var layoutThemeDark: KeyboardTheme? = null

    companion object {
        private const val TEXT_SIZE_RATIO = 0.4f
        private const val MAX_TEXT_WIDTH_RATIO = 0.8f
    }

    private fun performVibration() {
        if (!vibrationEnabled) return
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(android.os.VibrationEffect.createOneShot(vibrationStrength.toLong(), android.os.VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(vibrationStrength.toLong())
        }
    }

    // Appearance & Animation Properties (Configurable)
    var keyRadius = 24f
    var funcKeyRadius = 12f
    var shadowOffset = 3f
    var shadowAlpha = 30
    var animDuration = 200L
    var rippleExpandDuration = 350L
    var rippleFadeDuration = 200L

    private var currentPage: KeyboardPage? = null
    private var nextPage: KeyboardPage? = null
    private var slideOffset = 0f // 0f = currentPage fully visible, -1f = sliding left, 1f = sliding right

    private val keyDrawables = mutableListOf<KeyDrawable>()
    private val nextKeyDrawables = mutableListOf<KeyDrawable>()
    
    var activeStates: Map<String, Boolean> = emptyMap()
        set(value) { field = value; invalidate() }
        
    var theme: KeyboardTheme? = null
        set(value) { field = value; applyTheme(); invalidate() }

    var onKeyListener: ((KeyboardKey) -> Unit)? = null
    var onSwipeListener: ((Direction) -> Unit)? = null

    // Settings
    var swipeThreshold = 50 
    var horizontalSpacing = 6
    var verticalSpacing = 6
    var vibrationEnabled = true
    var vibrationStrength = 30

    enum class Direction { LEFT, RIGHT }

    private var colorBg = 0; private var colorKey = 0; private var colorFuncKey = 0
    private var colorText = 0; private var colorSticky = 0; private var colorStickyActive = 0
    private var colorStickyTextActive = 0
    private var colorRipple = 0
    private var colorFuncText = 0
    
    private val vibrator = context.getSystemService(android.os.Vibrator::class.java)

    private val shadowPaint = Paint().apply { isAntiAlias = true }
    private val keyPaint = Paint().apply { isAntiAlias = true }
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

    init { }

    fun getCurrentPageId(): String? = currentPage?.id

    private fun applyTheme() {
        val isDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        assetRes.refresh()

        // 1. 设置默认颜色 (从 Assets 加载)
        colorBg = assetRes.getColor("keyboard_background", Color.BLACK)
        colorKey = assetRes.getColor("key_background", Color.DKGRAY)
        colorFuncKey = assetRes.getColor("func_key_background", Color.GRAY)
        colorText = assetRes.getColor("key_text", Color.WHITE)
        colorFuncText = assetRes.getColor("func_key_text", Color.WHITE)
        colorSticky = assetRes.getColor("sticky_inactive_background", Color.GRAY)
        colorStickyActive = assetRes.getColor("sticky_active_background", Color.BLUE)
        colorStickyTextActive = assetRes.getColor("sticky_active_text", Color.WHITE)
        colorRipple = assetRes.getColor("ripple_color", Color.WHITE)

        // 2. 根据当前模式选择 JSON 覆盖 (Layout 级别覆盖)
        val activeTheme = if (isDark) layoutThemeDark ?: theme else layoutThemeLight ?: theme
        activeTheme?.let { t ->
            fun parseSafe(c: String?, default: Int): Int = try {
                if (c != null) Color.parseColor(c) else default
            } catch (e: Exception) { default }

            colorBg = parseSafe(t.background, colorBg)
            colorKey = parseSafe(t.keyBackground, colorKey)
            colorText = parseSafe(t.keyText, colorText)
            colorFuncKey = parseSafe(t.functionKeyBackground, colorFuncKey)
            colorFuncText = parseSafe(t.functionKeyText, colorFuncText)
            colorSticky = parseSafe(t.stickyInactiveBackground, colorSticky)
            colorStickyActive = parseSafe(t.stickyActiveBackground, colorStickyActive)
            colorStickyTextActive = parseSafe(t.stickyActiveText, colorStickyTextActive)
            colorRipple = parseSafe(t.rippleColor, colorRipple)
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
            animator.duration = animDuration
            animator.interpolator = DecelerateInterpolator()
            animator.addUpdateListener { 
                val fraction = it.animatedValue as Float
                slideOffset = end * fraction
                invalidate()
            }
            animator.addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    currentPage = page
                    layoutKeys(page, keyDrawables, width, height)
                    slideOffset = 0f
                    nextPage = null
                    invalidate()
                }
            })
            animator.start()
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        currentPage?.let { layoutKeys(it, keyDrawables, width, height) }
    }

    private fun layoutKeys(page: KeyboardPage, targetList: MutableList<KeyDrawable>, w: Int, h: Int) {
        if (w <= 0 || h <= 0 || page.rows.isEmpty()) return
        
        // Cancel existing animations before clearing
        for (kd in targetList) kd.cancelAnimators()
        targetList.clear()
        
        val rowHeight = h / page.rows.size.toFloat()
        var curY = 0f
        
        val hSpacing = horizontalSpacing.toFloat()
        val vSpacing = verticalSpacing.toFloat()

        for (row in page.rows) {
            val totalWeight = row.keys.sumOf { it.weight.toDouble() }.toFloat()
            if (totalWeight <= 0f) {
                curY += rowHeight
                continue
            }
            
            var curX = 0f
            for (key in row.keys) {
                val keyW = key.weight * (w / totalWeight)
                
                val rect = RectF(
                    curX + hSpacing / 2f, 
                    curY + vSpacing / 2f, 
                    curX + keyW - hSpacing / 2f, 
                    curY + rowHeight - vSpacing / 2f
                )
                
                var textSize = rowHeight * TEXT_SIZE_RATIO
                val label = key.label ?: ""
                if (label.isNotEmpty()) {
                    textPaint.textSize = textSize
                    val textW = textPaint.measureText(label)
                    val maxTextW = rect.width() * MAX_TEXT_WIDTH_RATIO
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
        var radius = keyRadius 
        var textColor = colorText

        when (effectiveStyle) {
            KeyStyle.FUNCTION -> { 
                paintColor = colorFuncKey
                textColor = colorFuncText
                radius = funcKeyRadius 
            }
            KeyStyle.STICKY -> {
                val isActive = key.id?.let { activeStates[it] } ?: false
                paintColor = if (isActive) colorStickyActive else colorSticky
                if (isActive) {
                    textColor = colorStickyTextActive
                }
                radius = funcKeyRadius
            }
            else -> { 
                if (key.type != KeyType.NORMAL) {
                    paintColor = colorFuncKey
                    textColor = colorFuncText
                } 
            }
        }

        shadowPaint.color = Color.argb(shadowAlpha, 0, 0, 0)
        val shadowOff = shadowOffset
        // 稍微在左右也扩出 1px，让阴影更丰满
        canvas.drawRoundRect(rect.left - 1f, rect.top + shadowOff, rect.right + 1f, rect.bottom + shadowOff, radius, radius, shadowPaint)
        keyPaint.color = paintColor
        canvas.drawRoundRect(rect, radius, radius, keyPaint)
        
        // Ripple Effect
        if (kd.rippleIntensity > 0f) {
            canvas.save()
            clipPath.rewind() 
            clipPath.addRoundRect(rect, radius, radius, Path.Direction.CW)
            try {
                canvas.clipPath(clipPath)
                ripplePaint.color = colorRipple
                val baseAlpha = Color.alpha(colorRipple)
                ripplePaint.alpha = (baseAlpha * kd.rippleIntensity).toInt()
                canvas.drawCircle(kd.rippleX, kd.rippleY, kd.rippleRadius, ripplePaint)
            } catch (e: Exception) {
                // Fallback for some older devices where clipPath might fail on hardware canvas
            }
            canvas.restore()
        }

        textPaint.color = textColor
        textPaint.textSize = kd.baseTextSize 
        
        val baseline = rect.centerY() - (textPaint.fontMetrics.bottom + textPaint.fontMetrics.top) / 2
        canvas.drawText(effectiveLabel, rect.centerX(), baseline, textPaint)
    }

    private val pointerMap = mutableMapOf<Int, KeyDrawable>()

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (gestureDetector.onTouchEvent(e)) return true
        
        val action = e.actionMasked
        val index = e.actionIndex
        val id = e.getPointerId(index)
        val x = e.getX(index)
        val y = e.getY(index)

        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val kd = keyDrawables.find { it.rect.contains(x, y) }
                kd?.let {
                    pointerMap[id] = it
                    it.onPressed(x, y)
                    performVibration()
                    invalidate()
                }
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until e.pointerCount) {
                    val pid = e.getPointerId(i)
                    val px = e.getX(i)
                    val py = e.getY(i)
                    val kd = pointerMap[pid]
                    if (kd != null && !kd.rect.contains(px, py)) {
                        kd.onReleased()
                        pointerMap.remove(pid)
                        invalidate()
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                pointerMap[id]?.let {
                    if (it.rect.contains(x, y)) {
                        onKeyListener?.invoke(it.key)
                    }
                    it.onReleased()
                    pointerMap.remove(id)
                    invalidate()
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                pointerMap.values.forEach { it.onReleased() }
                pointerMap.clear()
                invalidate()
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
                duration = rippleExpandDuration
                interpolator = DecelerateInterpolator()
                addUpdateListener { 
                    rippleRadius = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
            
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
                duration = rippleFadeDuration
                addUpdateListener {
                    rippleIntensity = it.animatedValue as Float
                    invalidate()
                }
                start()
            }
        }

        fun cancelAnimators() {
            radiusAnimator?.cancel()
            alphaAnimator?.cancel()
        }
    }
}
