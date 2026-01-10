package com.implus.input.manager

import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import com.implus.input.R
import com.implus.input.utils.Constants

/**
 * 负责处理输入法界面的静态 UI 元素着色与主题应用，减轻 Service 负担
 */
class UIManager(private val assetRes: AssetResourceManager) {

    fun applyThemeToStaticViews(root: View?) {
        val view = root ?: return
        
        val panelBg = assetRes.getColor("panel_background", android.graphics.Color.DKGRAY)
        val toolbarBg = assetRes.getColor("toolbar_background", android.graphics.Color.BLACK)
        val keyText = assetRes.getColor("key_text", android.graphics.Color.WHITE)
        val rippleColor = assetRes.getColor("ripple_color", Constants.DEFAULT_RIPPLE_COLOR)
        val accentError = assetRes.getColor("accent_error", android.graphics.Color.RED)
        val dividerColor = assetRes.getColor("divider_color", android.graphics.Color.LTGRAY)

        view.findViewById<View>(R.id.candidate_container)?.setBackgroundColor(toolbarBg)
        view.findViewById<View>(R.id.toolbar_view)?.parent?.let { (it as View).setBackgroundColor(panelBg) }
        view.findViewById<View>(R.id.clipboard_view)?.setBackgroundColor(panelBg)
        view.findViewById<View>(R.id.edit_pad_view)?.setBackgroundColor(panelBg)
        
        tintToolbarButtons(view, keyText)
        tintEditPadButtons(view, keyText, rippleColor, dividerColor)
        updateTextContent(view, keyText, accentError)
    }

    private fun tintToolbarButtons(root: View, color: Int) {
        val ids = listOf(R.id.btn_settings, R.id.btn_edit_mode, R.id.btn_clipboard, R.id.btn_close_keyboard)
        ids.forEach { id ->
            root.findViewById<ImageView>(id)?.setColorFilter(color)
        }
    }

    private fun tintEditPadButtons(root: View, color: Int, ripple: Int, divider: Int) {
        val arrows = listOf(R.id.btn_arrow_up, R.id.btn_arrow_down, R.id.btn_arrow_left, R.id.btn_arrow_right)
        arrows.forEach { id ->
            root.findViewById<ImageButton>(id)?.setColorFilter(color)
        }

        val materialButtons = listOf(R.id.btn_select_all, R.id.btn_copy, R.id.btn_pad_paste, R.id.btn_back_to_keyboard)
        materialButtons.forEach { id ->
            root.findViewById<com.google.android.material.button.MaterialButton>(id)?.let {
                it.setTextColor(color)
                it.rippleColor = android.content.res.ColorStateList.valueOf(ripple)
                if (id == R.id.btn_back_to_keyboard) {
                    it.strokeColor = android.content.res.ColorStateList.valueOf(divider)
                }
            }
        }
    }

    private fun updateTextContent(root: View, keyText: Int, errorText: Int) {
        root.findViewById<TextView>(R.id.clipboard_title)?.let {
            it.text = assetRes.getString("clipboard_title")
            it.setTextColor(keyText)
        }
        root.findViewById<Button>(R.id.btn_clear_clipboard)?.let {
            it.text = assetRes.getString("clipboard_clear")
            it.setTextColor(errorText)
        }

        root.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_select_all)?.text = 
            assetRes.getString("edit_select_all")
        root.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_copy)?.text = 
            assetRes.getString("edit_copy")
        root.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_pad_paste)?.text = 
            assetRes.getString("edit_paste")
        root.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_back_to_keyboard)?.text = 
            assetRes.getString("edit_back")
    }
}
