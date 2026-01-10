package com.implus.input

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.implus.input.manager.AssetResourceManager
import java.util.Locale

/**
 * MainActivity 是应用的引导和设置界面，而非输入法界面本身。
 * 用户通过此界面完成输入法的启用和基本配置。
 */
class MainActivity : AppCompatActivity() {

    private val assetRes by lazy { AssetResourceManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupButtons()
        setupLanguageSwitcher()
        applyTexts()
        
        findViewById<Button>(R.id.btn_open_settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun applyTexts() {
        assetRes.refresh()
        findViewById<TextView>(R.id.tv_welcome_title)?.text = assetRes.getString("welcome_title")
        findViewById<TextView>(R.id.tv_welcome_subtitle)?.text = assetRes.getString("welcome_subtitle")
        findViewById<TextView>(R.id.tv_test_label)?.text = assetRes.getString("test_input_label")
        findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.layout_test_input)?.hint = assetRes.getString("test_input_hint")
        findViewById<Button>(R.id.btn_open_settings)?.text = assetRes.getString("open_settings")
        updateButtonStates()
    }

    private fun setupButtons() {
        val btnEnable = findViewById<Button>(R.id.btn_enable_ime)
        val btnSelect = findViewById<Button>(R.id.btn_select_ime)

        btnEnable.setOnClickListener {
            val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
            startActivity(intent)
        }

        btnSelect.setOnClickListener {
            if (isImeEnabled()) {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.showInputMethodPicker()
            } else {
                Toast.makeText(this, assetRes.getString("toast_enable_first"), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupLanguageSwitcher() {
        val btnLanguage = findViewById<ImageView>(R.id.btn_language)
        btnLanguage.setOnClickListener { view ->
            showLanguageMenu(view)
        }
    }

    private fun showLanguageMenu(view: View) {
        val popup = PopupMenu(this, view)
        popup.menu.add(0, 0, 0, assetRes.getString("menu_lang_default"))
        popup.menu.add(0, 1, 1, assetRes.getString("menu_lang_en"))
        popup.menu.add(0, 2, 2, assetRes.getString("menu_lang_zh"))

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                0 -> setLocale(LocaleListCompat.getEmptyLocaleList()) // 跟随系统
                1 -> setLocale(LocaleListCompat.create(Locale.US))
                2 -> setLocale(LocaleListCompat.create(Locale.SIMPLIFIED_CHINESE))
            }
            true
        }
        popup.show()
    }

    private fun setLocale(locales: LocaleListCompat) {
        AppCompatDelegate.setApplicationLocales(locales)
        // Activity 会自动重建，无需手动 recreate
    }

    override fun onResume() {
        super.onResume()
        updateButtonStates()
    }

    private fun updateButtonStates() {
        val btnEnable = findViewById<Button>(R.id.btn_enable_ime)
        val btnSelect = findViewById<Button>(R.id.btn_select_ime)

        if (isImeEnabled()) {
            btnEnable?.text = assetRes.getString("step_1_done")
            btnEnable?.isEnabled = false
        } else {
            btnEnable?.text = assetRes.getString("step_1_enable")
            btnEnable?.isEnabled = true
        }

        if (isImeSelected()) {
            btnSelect?.text = assetRes.getString("step_2_done")
            btnSelect?.isEnabled = false
        } else {
            btnSelect?.text = assetRes.getString("step_2_select")
            btnSelect?.isEnabled = true
        }
    }

    private fun isImeEnabled(): Boolean {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val enabledMethods = imm.enabledInputMethodList
        return enabledMethods.any { it.packageName == packageName }
    }

    private fun isImeSelected(): Boolean {
        val currentImeId = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.DEFAULT_INPUT_METHOD
        )
        return currentImeId?.contains(packageName) == true
    }
}