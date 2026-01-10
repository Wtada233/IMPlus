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
import com.implus.input.layout.KeyboardKey
import com.implus.input.layout.KeyboardPage
import com.implus.input.layout.KeyboardRow
import com.implus.input.layout.KeyboardTheme
import com.implus.input.layout.KeyStyle
import com.implus.input.layout.KeyType
import com.implus.input.manager.AssetResourceManager
import com.implus.input.utils.Constants
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
        // Constants moved to Constants.kt or kept if truly local/default
        private const val DEFAULT_KEY_RADIUS = 24f
        private const val DEFAULT_FUNC_KEY_RADIUS = 12f
        private const val DEFAULT_SHADOW_OFFSET = 3f
        private const val DEFAULT_SHADOW_ALPHA = 30
        private const val DEFAULT_ANIM_DURATION = 200L
        private const val DEFAULT_RIPPLE_EXPAND_DURATION = 350L
        private const val DEFAULT_RIPPLE_FADE_DURATION = 200L
        
        private const val DEFAULT_SWIPE_THRESHOLD = 50
        private const val DEFAULT_SPACING = 6
        private const val DEFAULT_VIBRATION_STRENGTH = 30
        
        private const val FLING_VELOCITY_THRESHOLD = 100
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
    var keyRadius = DEFAULT_KEY_RADIUS
    var funcKeyRadius = DEFAULT_FUNC_KEY_RADIUS
    var shadowOffset = DEFAULT_SHADOW_OFFSET
    var shadowAlpha = DEFAULT_SHADOW_ALPHA
    var animDuration = DEFAULT_ANIM_DURATION
    var rippleExpandDuration = DEFAULT_RIPPLE_EXPAND_DURATION
    var rippleFadeDuration = DEFAULT_RIPPLE_FADE_DURATION

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
    var swipeThreshold = DEFAULT_SWIPE_THRESHOLD 
    var horizontalSpacing = DEFAULT_SPACING
    var verticalSpacing = DEFAULT_SPACING
    var vibrationEnabled = true
    var vibrationStrength = DEFAULT_VIBRATION_STRENGTH

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
            var handled = false
            if (e1 != null) {
                val diffX = e2.x - e1.x
                val diffY = e2.y - e1.y
                if (abs(diffX) > abs(diffY) && abs(diffX) > swipeThreshold && abs(velocityX) > FLING_VELOCITY_THRESHOLD) {
                    if (diffX > 0) onSwipeListener?.invoke(Direction.RIGHT)
                    else onSwipeListener?.invoke(Direction.LEFT)
                    handled = true
                }
            }
            return handled
        }
    })

    fun getCurrentPageId(): String? = currentPage?.id

    private fun applyTheme() {
        val isDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        assetRes.refresh()

        // 1. 设置默认颜色 (从 Assets 加载或使用 Constants 兜底)
        colorBg = assetRes.getColor("keyboard_background", Constants.COLOR_DEFAULT_BG)
        colorKey = assetRes.getColor("key_background", Constants.COLOR_DEFAULT_KEY_BG)
        colorFuncKey = assetRes.getColor("func_key_background", Constants.COLOR_DEFAULT_FUNC_KEY_BG)
        colorText = assetRes.getColor("key_text", Constants.COLOR_DEFAULT_KEY_TEXT)
        colorFuncText = assetRes.getColor("func_key_text", Constants.COLOR_DEFAULT_FUNC_KEY_TEXT)
        colorSticky = assetRes.getColor("sticky_inactive_background", Constants.COLOR_DEFAULT_STICKY_INACTIVE)
        colorStickyActive = assetRes.getColor("sticky_active_background", Constants.COLOR_DEFAULT_STICKY_ACTIVE)
        colorStickyTextActive = assetRes.getColor("sticky_active_text", Constants.COLOR_DEFAULT_STICKY_TEXT_ACTIVE)
        colorRipple = assetRes.getColor("ripple_color", Constants.DEFAULT_RIPPLE_COLOR)

        // 2. 根据当前模式选择 JSON 覆盖 (Layout 级别覆盖)
        val activeTheme = if (isDark) layoutThemeDark ?: theme else layoutThemeLight ?: theme
        activeTheme?.let { t ->
            colorBg = parseSafeColor(t.background, colorBg)
            colorKey = parseSafeColor(t.keyBackground, colorKey)
            colorText = parseSafeColor(t.keyText, colorText)
            colorFuncKey = parseSafeColor(t.functionKeyBackground, colorFuncKey)
            colorFuncText = parseSafeColor(t.functionKeyText, colorFuncText)
            colorSticky = parseSafeColor(t.stickyInactiveBackground, colorSticky)
            colorStickyActive = parseSafeColor(t.stickyActiveBackground, colorStickyActive)
            colorStickyTextActive = parseSafeColor(t.stickyActiveText, colorStickyTextActive)
            colorRipple = parseSafeColor(t.rippleColor, colorRipple)
        }
    }

    private fun parseSafeColor(colorStr: String?, default: Int): Int {
        return try {
            if (colorStr != null) Color.parseColor(colorStr) else default
        } catch (e: IllegalArgumentException) {
            android.util.Log.e("ImplusKeyboardView", "Color parse error: $colorStr", e)
            default
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
        
        targetList.forEach { it.cancelAnimators() }
        targetList.clear()
        
        val rowHeight = h / page.rows.size.toFloat()
        var curY = 0f
        
        for (row in page.rows) {
            layoutRow(row, targetList, curY, rowHeight, w)
            curY += rowHeight
        }
    }

    private fun layoutRow(row: KeyboardRow, targetList: MutableList<KeyDrawable>, curY: Float, rowHeight: Float, w: Int) {
        val totalWeight = row.keys.sumOf { it.weight.toDouble() }.toFloat()
        if (totalWeight <= 0f) return
        
        var curX = 0f
        val hSpacing = horizontalSpacing.toFloat()
        val vSpacing = verticalSpacing.toFloat()
        val spacingDivider = 2f

        for (key in row.keys) {
            val keyW = key.weight * (w / totalWeight)
            val rect = RectF(
                curX + hSpacing / spacingDivider, 
                curY + vSpacing / spacingDivider, 
                curX + keyW - hSpacing / spacingDivider, 
                curY + rowHeight - vSpacing / spacingDivider
            )
            
            targetList.add(createKeyDrawable(key, rect, rowHeight))
            curX += keyW
        }
    }

    private fun createKeyDrawable(key: KeyboardKey, rect: RectF, rowHeight: Float): KeyDrawable {
        var textSize = rowHeight * Constants.KEY_TEXT_SIZE_RATIO
        val label = key.label ?: ""
        if (label.isNotEmpty()) {
            textPaint.textSize = textSize
            val textW = textPaint.measureText(label)
            val maxTextW = rect.width() * Constants.KEY_MAX_TEXT_WIDTH_RATIO
            if (textW > maxTextW) {
                textSize *= (maxTextW / textW)
            }
        }
        return KeyDrawable(key, rect, textSize)
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
        
        val label = resolveKeyLabel(key)
        val style = resolveKeyStyle(key)

        var paintColor = colorKey
        var radius = keyRadius 
        var textColor = colorText

        when (style) {
            KeyStyle.FUNCTION -> { 
                paintColor = colorFuncKey
                textColor = colorFuncText
                radius = funcKeyRadius 
            }
            KeyStyle.STICKY -> {
                val isActive = key.id?.let { activeStates[it] } ?: false
                paintColor = if (isActive) colorStickyActive else colorSticky
                if (isActive) textColor = colorStickyTextActive
                radius = funcKeyRadius
            }
            else -> { 
                if (key.type != KeyType.NORMAL) {
                    paintColor = colorFuncKey
                    textColor = colorFuncText
                } 
            }
        }

        drawKeyShadow(canvas, rect, radius)
        drawKeyBackground(canvas, rect, radius, paintColor)
        drawRippleEffect(canvas, rect, radius, kd)
        drawKeyLabel(canvas, rect, label, textColor, kd.baseTextSize)
    }

    private fun resolveKeyLabel(key: KeyboardKey): String {
        var label = key.label ?: ""
        val overrides = key.overrides ?: return label
        for ((stateId, override) in overrides) {
            if (activeStates[stateId] == true) {
                override.label?.let { label = it }
            }
        }
        return label
    }

    private fun resolveKeyStyle(key: KeyboardKey): KeyStyle {
        var style = key.style
        val overrides = key.overrides ?: return style
        for ((stateId, override) in overrides) {
            if (activeStates[stateId] == true) {
                override.style?.let { style = it }
            }
        }
        return style
    }

    private fun drawKeyShadow(canvas: Canvas, rect: RectF, radius: Float) {
        val shadowColorAlpha = shadowAlpha
        shadowPaint.color = Color.argb(shadowColorAlpha, 0, 0, 0)
        val shadowOff = shadowOffset
        val horizontalExtra = 1f
        canvas.drawRoundRect(rect.left - horizontalExtra, rect.top + shadowOff, rect.right + horizontalExtra, rect.bottom + shadowOff, radius, radius, shadowPaint)
    }

    private fun drawKeyBackground(canvas: Canvas, rect: RectF, radius: Float, color: Int) {
        keyPaint.color = color
        canvas.drawRoundRect(rect, radius, radius, keyPaint)
    }

    private fun drawRippleEffect(canvas: Canvas, rect: RectF, radius: Float, kd: KeyDrawable) {
        if (kd.rippleIntensity <= 0f) return
        
        canvas.save()
        clipPath.rewind() 
        clipPath.addRoundRect(rect, radius, radius, Path.Direction.CW)
        try {
            canvas.clipPath(clipPath)
            ripplePaint.color = colorRipple
            val baseAlpha = Color.alpha(colorRipple)
            ripplePaint.alpha = (baseAlpha * kd.rippleIntensity).toInt()
            canvas.drawCircle(kd.rippleX, kd.rippleY, kd.rippleRadius, ripplePaint)
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            android.util.Log.e("ImplusKeyboardView", "Ripple clip error", e)
        }
        canvas.restore()
    }

    private fun drawKeyLabel(canvas: Canvas, rect: RectF, label: String, color: Int, textSize: Float) {
        textPaint.color = color
        textPaint.textSize = textSize 
        val baselineModifier = 2f
        val baseline = rect.centerY() - (textPaint.fontMetrics.bottom + textPaint.fontMetrics.top) / baselineModifier
        canvas.drawText(label, rect.centerX(), baseline, textPaint)
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
            val quickFadeMs = 50L
            alphaAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = quickFadeMs
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

