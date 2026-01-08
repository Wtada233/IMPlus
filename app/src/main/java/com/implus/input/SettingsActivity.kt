package com.implus.input

import android.content.Context
import android.os.Bundle
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        setupHeightControl()
        setupCloseOutside()
    }

    private fun setupHeightControl() {
        val seekBar = findViewById<SeekBar>(R.id.seekbar_height)
        val tvVal = findViewById<TextView>(R.id.tv_height_val)
        val prefs = getSharedPreferences("implus_prefs", Context.MODE_PRIVATE)
        
        val savedPercent = prefs.getInt("height_percent", 35)
        seekBar.progress = savedPercent
        tvVal.text = "$savedPercent%"

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val realProgress = if (progress < 20) 20 else progress
                tvVal.text = "$realProgress%"
                prefs.edit().putInt("height_percent", realProgress).apply()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupCloseOutside() {
        val switch = findViewById<SwitchMaterial>(R.id.switch_close_outside)
        val prefs = getSharedPreferences("implus_prefs", Context.MODE_PRIVATE)
        
        switch.isChecked = prefs.getBoolean("close_outside", false)
        
        switch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("close_outside", isChecked).apply()
        }
    }
}
