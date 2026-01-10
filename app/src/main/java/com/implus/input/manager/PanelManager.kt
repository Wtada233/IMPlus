package com.implus.input.manager

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.implus.input.R

/**
 * 专门负责管理输入法上方的各种功能面板（剪贴板、编辑区等）
 */
class PanelManager(
    private val root: View,
    private val onCommitText: (String) -> Unit,
    private val onBackToKeyboard: () -> Unit
) {
    var theme: com.implus.input.layout.KeyboardTheme? = null
    var themeLight: com.implus.input.layout.KeyboardTheme? = null
    var themeDark: com.implus.input.layout.KeyboardTheme? = null
    private val assetRes by lazy { AssetResourceManager(root.context) }
    
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
        
        val isDark = (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val activeTheme = if (isDark) themeDark ?: theme else themeLight ?: theme

        val textColorStr = activeTheme?.keyText
        val textColor = try {
            if (textColorStr != null) android.graphics.Color.parseColor(textColorStr) else assetRes.getColor("key_text", android.graphics.Color.WHITE)
        } catch (e: Exception) {
            assetRes.getColor("key_text", android.graphics.Color.WHITE)
        }

        clipboardView.findViewById<View>(R.id.btn_clear_clipboard)?.setOnClickListener {
            ClipboardHistoryManager.clear(context)
            refreshClipboard()
        }

        val history = ClipboardHistoryManager.getHistory(context)
        if (history.isEmpty()) {
            val emptyView = TextView(context).apply {
                text = assetRes.getString("clipboard_empty")
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
                    val dividerColor = assetRes.getColor("divider_color", android.graphics.Color.LTGRAY)
                    setBackgroundColor(dividerColor)
                    clipboardList.addView(this)
                }
            }
        }
    }
}
