package com.implus.input

import android.content.Context
import android.content.res.Configuration
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

class ImplusKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var currentPage: KeyboardPage? = null
    private val keyDrawables = mutableListOf<KeyDrawable>()
    
    var isShifted: Boolean = false; set(value) { field = value; invalidate() }
    var isCapsLock: Boolean = false; set(value) { field = value; invalidate() }
    var onKeyListener: ((KeyboardKey) -> Unit)? = null

    // 颜色配置
    private var colorBg = 0
    private var colorKey = 0
    private var colorFuncKey = 0
    private var colorText = 0
    private var colorSticky = 0
    private var colorStickyActive = 0
    private var colorShadow = Color.argb(40, 0, 0, 0)

    private val bgPaint = Paint()
    private val keyPaint = Paint().apply { style = Paint.Style.FILL }
    private val textPaint = Paint().apply { textAlign = Paint.Align.CENTER; isAntiAlias = true }
    private val shadowPaint = Paint().apply { style = Paint.Style.FILL }
    private val ripplePaint = Paint().apply { color = Color.argb(40, 255, 255, 255) }

    init {
        applyTheme()
    }

    private fun applyTheme() {
        val nightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val isDark = nightMode == Configuration.UI_MODE_NIGHT_YES

        if (isDark) {
            colorBg = Color.parseColor("#1A1C1E")
            colorKey = Color.parseColor("#303030")
            colorFuncKey = Color.parseColor("#252525")
            colorText = Color.WHITE
            colorSticky = Color.parseColor("#42474E")
            colorStickyActive = Color.parseColor("#D1E4FF")
        } else {
            colorBg = Color.parseColor("#F0F0F0")
            colorKey = Color.WHITE
            colorFuncKey = Color.parseColor("#E0E0E0")
            colorText = Color.BLACK
            colorSticky = Color.parseColor("#D0D0D0")
            colorStickyActive = Color.parseColor("#3F51B5")
        }
    }

    fun setPage(page: KeyboardPage) {
        this.currentPage = page
        layoutKeys(width, height)
        invalidate()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        layoutKeys(width, height)
    }

    private fun layoutKeys(viewWidth: Int, viewHeight: Int) {
        if (viewWidth <= 0 || viewHeight <= 0) return
        keyDrawables.clear()
        val page = currentPage ?: return
        
        val rows = page.rows
        if (rows.isEmpty()) return

        val rowHeight = viewHeight / rows.size.toFloat()
        var currentY = 0f

        for (row in rows) {
            val totalWeight = row.keys.sumOf { it.weight.toDouble() }.toFloat()
            if (totalWeight <= 0) continue
            val keyUnitWidth = viewWidth / totalWeight
            
            var currentX = 0f
            for (key in row.keys) {
                val keyWidth = key.weight * keyUnitWidth
                val rect = RectF(
                    currentX + 6f, 
                    currentY + 6f, 
                    currentX + keyWidth - 6f, 
                    currentY + rowHeight - 6f
                )
                keyDrawables.add(KeyDrawable(key, rect))
                currentX += keyWidth
            }
            currentY += rowHeight
        }
        textPaint.textSize = rowHeight * 0.35f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(colorBg)
        for (kd in keyDrawables) {
            drawKey(canvas, kd)
        }
    }

    private fun drawKey(canvas: Canvas, kd: KeyDrawable) {
        val key = kd.key
        val rect = kd.rect
        
        // 选择颜色和圆角
        var paintColor = colorKey
        var cornerRadius = 16f
        var textColor = colorText

        when (key.style) {
            KeyStyle.FUNCTION -> { paintColor = colorFuncKey; cornerRadius = 8f }
            KeyStyle.STICKY -> {
                val active = (key.sticky == "transient" && isShifted && !isCapsLock) ||
                             (key.sticky == "permanent" && isCapsLock)
                paintColor = if (active) colorStickyActive else colorSticky
                if (active && (colorStickyActive == Color.parseColor("#D1E4FF") || colorStickyActive == Color.parseColor("#3F51B5"))) {
                    textColor = if (colorStickyActive == Color.parseColor("#D1E4FF")) Color.BLACK else Color.WHITE
                }
                cornerRadius = 4f
            }
            else -> { if (key.type != KeyType.NORMAL) paintColor = colorFuncKey }
        }

        // 阴影
        canvas.drawRoundRect(rect.left, rect.top + 2f, rect.right, rect.bottom + 2f, cornerRadius, cornerRadius, shadowPaint.apply { color = colorShadow })
        
        // 背景
        keyPaint.color = paintColor
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, keyPaint)

        if (kd.isPressed) canvas.drawRoundRect(rect, cornerRadius, cornerRadius, ripplePaint)

        // 文字
        var label = key.label ?: ""
        if (isShifted || isCapsLock) {
            label = key.shiftedLabel ?: if (label.length == 1) label.uppercase() else label
        }

        textPaint.color = textColor
        val fontMetrics = textPaint.fontMetrics
        val baseline = rect.centerY() - (fontMetrics.bottom + fontMetrics.top) / 2
        canvas.drawText(label, rect.centerX(), baseline, textPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x; val y = event.y
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                pressedKey = keyDrawables.find { it.rect.contains(x, y) }
                pressedKey?.let { it.isPressed = true; invalidate() }
            }
            MotionEvent.ACTION_MOVE -> {
                if (pressedKey != null && !pressedKey!!.rect.contains(x, y)) {
                    pressedKey!!.isPressed = false; pressedKey = null; invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                pressedKey?.let {
                    if (it.rect.contains(x, y)) onKeyListener?.invoke(it.key)
                    it.isPressed = false; pressedKey = null; invalidate()
                }
            }
        }
        return true
    }

    private var pressedKey: KeyDrawable? = null
    private class KeyDrawable(val key: KeyboardKey, val rect: RectF) { var isPressed = false }
}