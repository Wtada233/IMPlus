package com.implus.input

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.implus.input.layout.KeyboardKey
import com.implus.input.layout.KeyboardPage
import com.implus.input.layout.KeyType
import com.implus.input.layout.KeyStyle

/**
 * 现代化的键盘视图
 * 支持 JSON 动态布局渲染
 */
class ImplusKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var currentPage: KeyboardPage? = null
    private val keyDrawables = mutableListOf<KeyDrawable>()
    
    // State
    var isShifted: Boolean = false
        set(value) {
            field = value
            invalidate()
        }
    var isCapsLock: Boolean = false
        set(value) {
            field = value
            invalidate()
        }
    
    // 监听器: 回调完整的 Key 对象以便 Service 处理复杂逻辑
    var onKeyListener: ((com.implus.input.layout.KeyboardKey) -> Unit)? = null

    // 画笔
    private val bgPaint = Paint().apply { color = Color.parseColor("#ECEFF1") }
    private val keyBgPaint = Paint().apply { color = Color.WHITE; style = Paint.Style.FILL }
    private val keyFuncBgPaint = Paint().apply { color = Color.parseColor("#CFD8DC"); style = Paint.Style.FILL }
    private val keyStickyBgPaint = Paint().apply { color = Color.parseColor("#D1C4E9"); style = Paint.Style.FILL } // Light Purple
    private val keyStickyLockedBgPaint = Paint().apply { color = Color.parseColor("#9575CD"); style = Paint.Style.FILL } // Deep Purple
    
    private val textPaint = Paint().apply {
        color = Color.BLACK
        textSize = 48f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }
    private val ripplePaint = Paint().apply { color = Color.parseColor("#40000000") }

    fun setPage(page: KeyboardPage) {
        this.currentPage = page
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Respect parent measurements
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        
        val targetHeight = if (heightMode == MeasureSpec.EXACTLY) {
            heightSize
        } else {
             (280 * resources.displayMetrics.density).toInt()
        }
        setMeasuredDimension(width, targetHeight)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        layoutKeys(width, height)
    }

    private fun layoutKeys(viewWidth: Int, viewHeight: Int) {
        keyDrawables.clear()
        val page = currentPage ?: return
        
        val rows = page.rows
        if (rows.isEmpty()) return

        val rowHeight = viewHeight / rows.size.toFloat()
        var currentY = 0f

        for (row in rows) {
            // 计算当前行的总权重
            val totalWeight = row.keys.sumOf { it.weight.toDouble() }.toFloat()
            val keyUnitWidth = viewWidth / totalWeight
            
            var currentX = 0f
            for (key in row.keys) {
                val keyWidth = key.weight * keyUnitWidth
                val rect = RectF(
                    currentX + 4f, 
                    currentY + 6f, 
                    currentX + keyWidth - 4f, 
                    currentY + rowHeight - 6f
                )
                
                keyDrawables.add(KeyDrawable(key, rect))
                currentX += keyWidth
            }
            currentY += rowHeight
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // 绘制背景
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // 绘制所有按键
        for (kd in keyDrawables) {
            drawKey(canvas, kd)
        }
    }

    private fun drawKey(canvas: Canvas, kd: KeyDrawable) {
        val rect = kd.rect
        val key = kd.key
        
        // Determine Paint and Corner Radius based on style and state
        val paint: Paint
        val cornerRadius: Float
        
        when (key.style) {
            KeyStyle.NORMAL -> {
                paint = keyBgPaint
                cornerRadius = 12f
            }
            KeyStyle.FUNCTION -> {
                paint = keyFuncBgPaint
                cornerRadius = 6f
            }
            KeyStyle.STICKY -> {
                // Check if this specific sticky key is active
                if ((key.sticky == "transient" && isShifted && !isCapsLock) ||
                    (key.sticky == "permanent" && isCapsLock)) {
                    paint = keyStickyLockedBgPaint
                } else {
                    paint = keyStickyBgPaint
                }
                cornerRadius = 4f
            }
        }
        
        // If type is modifier/func but style is normal, fallback
        val finalPaint = if (key.type != KeyType.NORMAL && key.style == KeyStyle.NORMAL) {
             keyFuncBgPaint
        } else {
             paint
        }

        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, finalPaint)

        if (kd.isPressed) {
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, ripplePaint)
        }

        // Determine Text
        var label = key.label ?: ""
        
        // Handle Shift Label Switching
        if (isShifted || isCapsLock) {
            if (key.shiftedLabel != null) {
                label = key.shiftedLabel
            } else if (label.length == 1 && label[0].isLowerCase()) {
                label = label.uppercase()
            }
        }

        val fontMetrics = textPaint.fontMetrics
        val baseline = rect.centerY() - (fontMetrics.bottom + fontMetrics.top) / 2
        canvas.drawText(label, rect.centerX(), baseline, textPaint)
    }

    private var pressedKey: KeyDrawable? = null

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val target = keyDrawables.find { it.rect.contains(x, y) }
                if (target != null) {
                    pressedKey = target
                    target.isPressed = true
                    invalidate() // 触发重绘显示按下效果
                }
            }
            MotionEvent.ACTION_MOVE -> {
                // 如果滑出按键范围，取消按下状态
                if (pressedKey != null && !pressedKey!!.rect.contains(x, y)) {
                    pressedKey!!.isPressed = false
                    pressedKey = null
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                if (pressedKey != null && pressedKey!!.rect.contains(x, y)) {
                    // 触发按键事件
                    onKeyListener?.invoke(pressedKey!!.key)
                    pressedKey!!.isPressed = false
                    pressedKey = null
                    invalidate()
                }
            }
        }
        return true
    }

    // 内部类保存绘制状态
    private class KeyDrawable(val key: KeyboardKey, val rect: RectF) {
        var isPressed: Boolean = false
    }
}