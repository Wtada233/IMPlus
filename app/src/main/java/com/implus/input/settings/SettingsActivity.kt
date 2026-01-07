package com.implus.input.settings

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.implus.input.databinding.ActivitySettingsBinding

/**
 * 输入法设置界面
 */
class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // TODO: 实现设置逻辑和初始化向导
    }
}
