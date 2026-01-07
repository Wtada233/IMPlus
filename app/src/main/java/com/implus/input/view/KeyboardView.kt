package com.implus.input.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.google.android.material.color.MaterialColors
import com.implus.input.model.KeyDefinition
import com.implus.input.model.KeyboardLayout

/**
 * 核心键盘视图，负责渲染 JSON 定义的布局并处理触摸事件
 */
class KeyboardView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var layout: KeyboardLayout? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    private val keyRect = RectF()
    
    // 涟漪动画相关
    private var rippleCenterX = 0f
    private var rippleCenterY = 0f
    private var rippleRadius = 0f
    private var rippleAlpha = 0
    private var rippleAnimator: ValueAnimator? = null
    private var activeRippleKey: KeyDefinition? = null

    // 缓存主题颜色
    private val colorPrimaryContainer by lazy { MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimaryContainer, Color.LTGRAY) }
    private val colorOnPrimaryContainer by lazy { MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnPrimaryContainer, Color.BLACK) }
    private val colorSurface by lazy { MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurface, Color.WHITE) }
    private val colorOnSurface by lazy { MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface, Color.BLACK) }

    var isHapticFeedbackEnabledCustom: Boolean = true
    var isShifted: Boolean = false
        set(value) {
            field = value
            invalidate()
        }

    interface OnKeyListener {
        fun onKey(key: KeyDefinition)
    }

    private var listener: OnKeyListener? = null
    private var pressedKey: KeyDefinition? = null

    fun setOnKeyListener(listener: OnKeyListener) {
        this.listener = listener
    }

    fun setLayout(keyboardLayout: KeyboardLayout) {
        this.layout = keyboardLayout
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val layout = layout ?: return

        val totalHeight = height.toFloat()
        val rowCount = layout.rows.size
        val keyHeight = totalHeight / rowCount

        var currentY = 0f
        for (row in layout.rows) {
            val totalWeight = row.keys.sumOf { it.weight.toDouble() }.toFloat()
            var currentX = 0f
            
            for (key in row.keys) {
                val currentKeyWidth = (key.weight / totalWeight) * width
                drawKey(canvas, key, currentX, currentY, currentKeyWidth, keyHeight)
                currentX += currentKeyWidth
            }
            currentY += keyHeight
        }
    }

    private fun drawKey(canvas: Canvas, key: KeyDefinition, x: Float, y: Float, w: Float, h: Float) {
        val padding = 6f
        keyRect.set(x + padding, y + padding, x + w - padding, y + h - padding)
        
        // 1. 绘制按键背景
        paint.color = when {
            key.functional -> {
                val base = colorPrimaryContainer
                Color.argb(40, Color.red(base), Color.green(base), Color.blue(base))
            }
            else -> colorSurface
        }
        canvas.drawRoundRect(keyRect, 16f, 16f, paint)

        // 2. 绘制涟漪效果
        if (key == activeRippleKey && rippleAlpha > 0) {
            paint.color = colorPrimaryContainer
            paint.alpha = rippleAlpha
            canvas.save()
            canvas.clipRect(keyRect)
            canvas.drawCircle(rippleCenterX, rippleCenterY, rippleRadius, paint)
            canvas.restore()
            paint.alpha = 255
        }

        // 3. 如果是常按状态且没有动画，显示一个静态深色背景
        if (key == pressedKey && key != activeRippleKey) {
            paint.color = colorPrimaryContainer
            paint.alpha = 100
            canvas.drawRoundRect(keyRect, 16f, 16f, paint)
            paint.alpha = 255
        }

        // 4. 绘制按键文字
        paint.color = colorOnSurface
        paint.textSize = h * 0.35f
        
        val label = key.label ?: ""
        val displayLabel = if (isShifted && label.length == 1 && label[0].isLetter()) {
            label.uppercase()
        } else {
            label
        }
        
        val fontMetrics = paint.fontMetrics
        val centerY = keyRect.centerY() - (fontMetrics.ascent + fontMetrics.descent) / 2
        canvas.drawText(displayLabel, keyRect.centerX(), centerY, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val key = findKeyAt(x, y)
                if (key != null) {
                    if (isHapticFeedbackEnabledCustom) {
                        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    }
                    pressedKey = key
                    startRipple(x, y, key)
                    invalidate()
                }
            }
            MotionEvent.ACTION_MOVE -> {
                val key = findKeyAt(x, y)
                if (key != pressedKey) {
                    pressedKey = key
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                pressedKey?.let { listener?.onKey(it) }
                pressedKey = null
                invalidate()
                performClick()
            }
            MotionEvent.ACTION_CANCEL -> {
                pressedKey = null
                invalidate()
            }
        }
        return true
    }

    private fun startRipple(x: Float, y: Float, key: KeyDefinition) {
        rippleCenterX = x
        rippleCenterY = y
        activeRippleKey = key
        
        rippleAnimator?.cancel()
        rippleAnimator = ValueAnimator.ofFloat(0f, width * 0.2f).apply {
            duration = 250
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                rippleRadius = animator.animatedValue as Float
                rippleAlpha = (255 * (1 - animator.animatedFraction)).toInt()
                invalidate()
            }
            start()
        }
    }

    private fun findKeyAt(x: Float, y: Float): KeyDefinition? {
        val layout = layout ?: return null
        val rowCount = layout.rows.size
        val rowIndex = (y / (height / rowCount)).toInt().coerceIn(0, rowCount - 1)
        
        val row = layout.rows[rowIndex]
        val totalWeight = row.keys.sumOf { it.weight.toDouble() }.toFloat()
        
        var currentX = 0f
        for (key in row.keys) {
            val currentKeyWidth = (key.weight / totalWeight) * width
            if (x >= currentX && x <= currentX + currentKeyWidth) {
                return key
            }
            currentX += currentKeyWidth
        }
        return null
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }
}