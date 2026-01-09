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
        
        clipboardView.findViewById<View>(R.id.btn_clear_clipboard).setOnClickListener {
            ClipboardHistoryManager.clear(context)
            refreshClipboard()
        }

        val history = ClipboardHistoryManager.getHistory(context)
        if (history.isEmpty()) {
            val emptyView = TextView(context).apply {
                text = context.getString(R.string.clipboard_empty)
                setTextColor(android.graphics.Color.WHITE)
                setPadding(32, 32, 32, 32)
            }
            clipboardList.addView(emptyView)
        } else {
            for (text in history) {
                val tv = TextView(context).apply {
                    this.text = text
                    setTextColor(android.graphics.Color.WHITE)
                    textSize = 16f
                    setPadding(32, 12, 32, 12)
                    setBackgroundResource(android.R.drawable.list_selector_background)
                    setOnClickListener {
                        onCommitText(text)
                        switchPanel(PanelType.KEYBOARD)
                    }
                }
                clipboardList.addView(tv)
                
                View(context).apply {
                    layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1)
                    setBackgroundColor(context.getColor(R.color.divider_dark))
                    clipboardList.addView(this)
                }
            }
        }
    }
}
