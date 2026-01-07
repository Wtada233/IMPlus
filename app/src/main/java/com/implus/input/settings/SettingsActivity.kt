package com.implus.input.settings

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import com.implus.input.databinding.ActivitySettingsBinding

/**
 * 输入法设置界面
 */
class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding

    override fun onResume() {
        super.onResume()
        updateButtonStates()
    }

    private fun updateButtonStates() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        val enabledIds = imm.enabledInputMethodList.map { it.id }
        val isEnabled = enabledIds.contains("$packageName/.ImplusInputMethodService")
        
        binding.btnEnableIme.isEnabled = !isEnabled
        binding.btnEnableIme.text = if (isEnabled) "IMPlus 已启用" else "第一步：启用 IMPlus"
        
        val currentIme = Settings.Secure.getString(contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
        val isSelected = currentIme == "$packageName/.ImplusInputMethodService"
        
        binding.btnSelectIme.isEnabled = isEnabled && !isSelected
        binding.btnSelectIme.text = if (isSelected) "IMPlus 当前正在使用" else "第二步：切换到 IMPlus"
    }
}
