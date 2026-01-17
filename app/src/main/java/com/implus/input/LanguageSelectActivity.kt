package com.implus.input

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.implus.input.layout.LayoutManager
import com.implus.input.manager.SettingsManager
import com.implus.input.manager.AssetResourceManager

/**
 * 专门用于处理输入法语言选择的透明 Activity
 */
class LanguageSelectActivity : AppCompatActivity() {

    private val settings by lazy { SettingsManager(this) }
    private val assetRes by lazy { AssetResourceManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 设置透明背景（在主题中定义，或通过代码）
        showLanguageDialog()
    }

    private fun showLanguageDialog() {
        val languages = LayoutManager.getAvailableLanguages(this)
        
        // 构造列表：第一个是“跟随系统”，后面是具体语言
        val items = mutableListOf<String>()
        items.add(assetRes.getString("menu_lang_default"))
        items.addAll(languages.map { it.name })
        
        val builder = com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
        builder.setTitle(assetRes.getString("select_language"))
        builder.setItems(items.toTypedArray()) { _, which ->
            if (which == 0) {
                // 选择跟随系统：我们将 ID 设为空，Service 逻辑会自动取当前系统 Locale
                // 或者我们可以定义一个特殊的常量
                settings.putString(SettingsManager.KEY_CURRENT_LANG, "")
            } else {
                val selected = languages[which - 1]
                settings.putString(SettingsManager.KEY_CURRENT_LANG, selected.id)
            }
            finish()
        }
        builder.setOnCancelListener { finish() }
        builder.show()
    }
}
