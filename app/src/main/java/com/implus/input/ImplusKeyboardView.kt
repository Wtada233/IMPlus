package com.implus.input

import android.animation.ValueAnimator
import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
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

    // 可配置的滑动阈值，默认 50px (更灵敏)
    var swipeThreshold = 50 

    enum class Direction { LEFT, RIGHT }

    private var colorBg = 0; private var colorKey = 0; private var colorFuncKey = 0
    private var colorText = 0; private var colorSticky = 0; private var colorStickyActive = 0
    private val shadowPaint = Paint(); private val bgPaint = Paint(); private val keyPaint = Paint()
    private val textPaint = Paint().apply { textAlign = Paint.Align.CENTER; isAntiAlias = true }
    private val ripplePaint = Paint().apply { color = Color.argb(60, 255, 255, 255) }

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
            colorBg = Color.parseColor("#1A1C1E"); colorKey = Color.parseColor("#303030")
            colorFuncKey = Color.parseColor("#252525"); colorText = Color.WHITE
            colorSticky = Color.parseColor("#42474E"); colorStickyActive = Color.parseColor("#D1E4FF")
        } else {
            colorBg = Color.parseColor("#F0F0F0"); colorKey = Color.WHITE
            colorFuncKey = Color.parseColor("#E0E0E0"); colorText = Color.BLACK
            colorSticky = Color.parseColor("#D0D0D0"); colorStickyActive = Color.parseColor("#3F51B5")
        }
    }

    fun setPage(page: KeyboardPage, animateDirection: Direction? = null) {
        if (animateDirection == null) {
            this.currentPage = page
            layoutKeys(page, keyDrawables, width, height)
            invalidate()
        } else {
            this.nextPage = page
            layoutKeys(page, nextKeyDrawables, width, height)
            
            val start = 0f
            val end = if (animateDirection == Direction.LEFT) -1f else 1f // Left swipe means content moves left
            
            val animator = ValueAnimator.ofFloat(0f, 1f)
            animator.duration = 200
            animator.interpolator = DecelerateInterpolator()
            animator.addUpdateListener { 
                val fraction = it.animatedValue as Float
                slideOffset = end * fraction
                invalidate()
            }
            animator.start()
            
            // 动画结束后切换数据
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
        for (row in page.rows) {
            val totalWeight = row.keys.sumOf { it.weight.toDouble() }.toFloat()
            var curX = 0f
            for (key in row.keys) {
                val keyW = key.weight * (w / totalWeight)
                targetList.add(KeyDrawable(key, RectF(curX + 6f, curY + 6f, curX + keyW - 6f, curY + rowHeight - 6f)))
                curX += keyW
            }
            curY += rowHeight
        }
        textPaint.textSize = rowHeight * 0.35f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(colorBg)

        // 绘制当前页 (Current Page)
        // slideOffset < 0 (向左滑): 当前页向左移 (-w * offset)
        // slideOffset > 0 (向右滑): 当前页向右移 (+w * offset)
        
        val w = width.toFloat()
        
        if (slideOffset != 0f && nextPage != null) {
            // 正在动画中
            val currentTransX = slideOffset * w
            
            // 绘制当前页
            canvas.save()
            canvas.translate(currentTransX, 0f)
            for (kd in keyDrawables) drawKey(canvas, kd)
            canvas.restore()
            
            // 绘制下一页
            // 如果 slideOffset < 0 (左移), next page 在右边 (w)
            // 如果 slideOffset > 0 (右移), next page 在左边 (-w)
            val nextStartX = if (slideOffset < 0) w else -w
            canvas.save()
            canvas.translate(nextStartX + currentTransX, 0f)
            for (kd in nextKeyDrawables) drawKey(canvas, kd)
            canvas.restore()
            
        } else {
            // 静态绘制
            for (kd in keyDrawables) drawKey(canvas, kd)
        }
    }

    private fun drawKey(canvas: Canvas, kd: KeyDrawable) {
        val key = kd.key
        val rect = kd.rect
        
        // 1. 获取当前有效属性 (应用 Overrides)
        var effectiveLabel = key.label ?: ""
        var effectiveStyle = key.style
        
        key.overrides?.forEach { (stateId, override) ->
            if (activeStates[stateId] == true) {
                override.label?.let { effectiveLabel = it }
                override.style?.let { effectiveStyle = it }
            }
        }

        // 2. 确定颜色
        var paintColor = colorKey
        var radius = 24f 
        var textColor = colorText

        when (effectiveStyle) {
            KeyStyle.FUNCTION -> { paintColor = colorFuncKey; radius = 12f }
            KeyStyle.STICKY -> {
                val isActive = key.id?.let { activeStates[it] } ?: false
                paintColor = if (isActive) colorStickyActive else colorSticky
                if (isActive) {
                    textColor = if (paintColor == Color.parseColor("#D1E4FF")) Color.BLACK else Color.WHITE
                }
                radius = 12f
            }
            else -> { if (key.type != KeyType.NORMAL) paintColor = colorFuncKey }
        }

        // 3. 绘制
        shadowPaint.color = Color.argb(30, 0, 0, 0)
        canvas.drawRoundRect(rect.left, rect.top + 3f, rect.right, rect.bottom + 3f, radius, radius, shadowPaint)
        keyPaint.color = paintColor
        canvas.drawRoundRect(rect, radius, radius, keyPaint)
        
        if (kd.isPressed) {
            canvas.drawRoundRect(rect, radius, radius, ripplePaint)
        }

        // 4. 文字缩放与绘制
        textPaint.color = textColor
        val maxW = rect.width() * 0.9f
        val oldSize = textPaint.textSize
        val measuredW = textPaint.measureText(effectiveLabel)
        if (measuredW > maxW) textPaint.textSize = oldSize * (maxW / measuredW)
        
        val baseline = rect.centerY() - (textPaint.fontMetrics.bottom + textPaint.fontMetrics.top) / 2
        canvas.drawText(effectiveLabel, rect.centerX(), baseline, textPaint)
        textPaint.textSize = oldSize
    }

    private var pressedKey: KeyDrawable? = null
    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (gestureDetector.onTouchEvent(e)) return true
        
        val x = e.x; val y = e.y
        when (e.action) {
            MotionEvent.ACTION_DOWN -> { 
                pressedKey = keyDrawables.find { it.rect.contains(x, y) }
                pressedKey?.let { it.isPressed = true; invalidate() } 
            }
            MotionEvent.ACTION_UP -> { 
                pressedKey?.let { 
                    if (it.rect.contains(x, y)) onKeyListener?.invoke(it.key)
                    it.isPressed = false
                    pressedKey = null
                    invalidate() 
                } 
            }
            MotionEvent.ACTION_CANCEL -> {
                pressedKey?.let { it.isPressed = false; pressedKey = null; invalidate() }
            }
            MotionEvent.ACTION_MOVE -> { 
                if (pressedKey != null && !pressedKey!!.rect.contains(x, y)) { 
                    pressedKey!!.isPressed = false
                    pressedKey = null
                    invalidate() 
                } 
            }
        }
        return true
    }
    private class KeyDrawable(val key: KeyboardKey, val rect: RectF) { var isPressed = false }
}
