package com.implus.input.manager

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.implus.input.R
import com.implus.input.manager.ClipboardHistoryManager

/**
 * 专门负责管理输入法上方的各种功能面板（剪贴板、编辑区等）
 */
class PanelManager(
    private val root: View,
    private val onCommitText: (String) -> Unit,
    private val onBackToKeyboard: () -> Unit
) {
    var theme: com.implus.input.layout.KeyboardTheme? = null
    
    private val keyboardView = root.findViewById<View>(R.id.keyboard_view)
    private val editPadView = root.findViewById<View>(R.id.edit_pad_view)
    private val clipboardView = root.findViewById<View>(R.id.clipboard_view)
    private val clipboardList = root.findViewById<ViewGroup>(R.id.clipboard_list)

    enum class PanelType { KEYBOARD, EDIT, CLIPBOARD }

    fun switchPanel(type: PanelType) {
        keyboardView.visibility = if (type == PanelType.KEYBOARD) View.VISIBLE else View.GONE
        editPadView.visibility = if (type == PanelType.EDIT) View.VISIBLE else View.GONE
        clipboardView.visibility = if (type == PanelType.CLIPBOARD) View.VISIBLE else View.GONE
        
        if (type == PanelType.CLIPBOARD) {
            refreshClipboard()
        }
    }

    private fun refreshClipboard() {
        val context = root.context
        clipboardList.removeAllViews()
        
        val textColorStr = theme?.keyText
        val textColor = try {
            if (textColorStr != null) android.graphics.Color.parseColor(textColorStr) else android.graphics.Color.WHITE
        } catch (e: Exception) {
            android.graphics.Color.WHITE
        }

        clipboardView.findViewById<View>(R.id.btn_clear_clipboard).setOnClickListener {
            ClipboardHistoryManager.clear(context)
            refreshClipboard()
        }

        val history = ClipboardHistoryManager.getHistory(context)
        if (history.isEmpty()) {
            val emptyView = TextView(context).apply {
                text = context.getString(R.string.clipboard_empty)
                setTextColor(textColor)
                setPadding(32, 32, 32, 32)
            }
            clipboardList.addView(emptyView)
        } else {
            val paddingHorizontal = (16 * context.resources.displayMetrics.density).toInt()
            val paddingVertical = (12 * context.resources.displayMetrics.density).toInt()

            for (text in history) {
                val tv = TextView(context).apply {
                    this.text = text
                    setTextColor(textColor)
                    textSize = 16f
                    setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical)
                    setBackgroundResource(android.R.drawable.list_selector_background)
                    setOnClickListener {
                        onCommitText(text)
                        switchPanel(PanelType.KEYBOARD)
                    }
                }
                clipboardList.addView(tv)
                
                View(context).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1)
                    val dividerColor = try {
                        if (theme?.background != null) {
                            // 稍微比背景亮一点作为分割线，或者使用预定义的 divider_dark
                            context.getColor(R.color.divider_dark)
                        } else context.getColor(R.color.divider_dark)
                    } catch (e: Exception) { context.getColor(R.color.divider_dark) }
                    setBackgroundColor(dividerColor)
                    clipboardList.addView(this)
                }
            }
        }
    }
}
