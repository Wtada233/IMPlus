package com.implus.input.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
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
    private var keyWidth: Float = 0f
    private var keyHeight: Float = 0f

    var hapticFeedbackEnabled: Boolean = true
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
        keyHeight = totalHeight / rowCount

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
        val padding = 4f
        keyRect.set(x + padding, y + padding, x + w - padding, y + h - padding)
        
        // 绘制按键背景，如果是当前按下的键则改变颜色
        paint.color = when {
            key == pressedKey -> Color.DKGRAY
            key.functional -> Color.LTGRAY
            else -> Color.WHITE
        }
        canvas.drawRoundRect(keyRect, 8f, 8f, paint)

        // 绘制按键文字
        paint.color = if (key == pressedKey) Color.WHITE else Color.BLACK
        paint.textSize = h * 0.4f
        
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
                    if (hapticFeedbackEnabled) {
                        performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    }
                    pressedKey = key
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
