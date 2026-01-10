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
    private val onCommitText: (String) -> Unit
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
        val textColor = parseTextColor(textColorStr)

        clipboardView.findViewById<View>(R.id.btn_clear_clipboard)?.setOnClickListener {
            ClipboardHistoryManager.clear(context)
            refreshClipboard()
        }

        val history = ClipboardHistoryManager.getHistory(context)
        if (history.isEmpty()) {
            addEmptyView(context, textColor)
        } else {
            addHistoryItems(context, history, textColor)
        }
    }

    private fun parseTextColor(textColorStr: String?): Int {
        return try {
            if (textColorStr != null) android.graphics.Color.parseColor(textColorStr) 
            else assetRes.getColor("key_text", android.graphics.Color.WHITE)
        } catch (e: IllegalArgumentException) {
            android.util.Log.e("PanelManager", "Color parse error: $textColorStr", e)
            assetRes.getColor("key_text", android.graphics.Color.WHITE)
        }
    }

    private fun addEmptyView(context: android.content.Context, textColor: Int) {
        val emptyView = TextView(context).apply {
            text = assetRes.getString("clipboard_empty")
            setTextColor(textColor)
            setPadding(
                Constants.PANEL_EMPTY_VIEW_PADDING, 
                Constants.PANEL_EMPTY_VIEW_PADDING, 
                Constants.PANEL_EMPTY_VIEW_PADDING, 
                Constants.PANEL_EMPTY_VIEW_PADDING
            )
        }
        clipboardList.addView(emptyView)
    }

    private fun addHistoryItems(context: android.content.Context, history: List<String>, textColor: Int) {
        val density = context.resources.displayMetrics.density
        val padH = (Constants.PANEL_ITEM_PADDING_HORIZONTAL * density).toInt()
        val padV = (Constants.PANEL_ITEM_PADDING_VERTICAL * density).toInt()

        for (text in history) {
            val tv = TextView(context).apply {
                this.text = text
                setTextColor(textColor)
                textSize = Constants.PANEL_ITEM_TEXT_SIZE
                setPadding(padH, padV, padH, padV)
                setBackgroundResource(android.R.drawable.list_selector_background)
                setOnClickListener {
                    onCommitText(text)
                    switchPanel(PanelType.KEYBOARD)
                }
            }
            clipboardList.addView(tv)
            
            val divider = View(context).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Constants.PANEL_DIVIDER_HEIGHT)
                val dividerColor = assetRes.getColor("divider_color", Constants.COLOR_DIVIDER_DEFAULT)
                setBackgroundColor(dividerColor)
            }
            clipboardList.addView(divider)
        }
    }
}

