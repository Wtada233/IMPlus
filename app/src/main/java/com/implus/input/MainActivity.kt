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
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

/**
 * 引导界面 (Init 界面)
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupButtons()
        setupLanguageSwitcher()
        
        findViewById<Button>(R.id.btn_open_settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
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
                Toast.makeText(this, R.string.toast_enable_first, Toast.LENGTH_SHORT).show()
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
        // 动态添加菜单项
        popup.menu.add(0, 0, 0, "System Default (跟随系统)")
        popup.menu.add(0, 1, 1, "English")
        popup.menu.add(0, 2, 2, "简体中文")

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
            btnEnable.text = getString(R.string.step_1_done)
            btnEnable.isEnabled = false
        } else {
            btnEnable.text = getString(R.string.step_1_enable)
            btnEnable.isEnabled = true
        }

        if (isImeSelected()) {
            btnSelect.text = getString(R.string.step_2_done)
            btnSelect.isEnabled = false
        } else {
            btnSelect.text = getString(R.string.step_2_select)
            btnSelect.isEnabled = true
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