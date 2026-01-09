package com.implus.input

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial
import com.implus.input.layout.*

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        setupHeightControl()
        setupCandidateHeightControl()
        setupCloseOutside()
        setupLanguageSettings()
    }

    private fun setupLanguageSettings() {
        val spinner = findViewById<Spinner>(R.id.spinner_languages)
        val switchPc = findViewById<SwitchMaterial>(R.id.switch_pc_layout)
        val prefs = getSharedPreferences("implus_prefs", Context.MODE_PRIVATE)

        val languages = LayoutManager.getAvailableLanguages(this)
        val names = languages.map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, names)
        spinner.adapter = adapter

        val currentLangId = prefs.getString("current_lang", "en")
        val currentIndex = languages.indexOfFirst { it.id == currentLangId }.coerceAtLeast(0)
        spinner.setSelection(currentIndex)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selected = languages[position]
                prefs.edit().putString("current_lang", selected.id).apply()
                
                // 更新该语言的 PC 布局开关状态
                switchPc.isChecked = prefs.getBoolean("use_pc_layout_${selected.id}", true)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        switchPc.setOnCheckedChangeListener { _, isChecked ->
            val selectedLangId = languages[spinner.selectedItemPosition].id
            prefs.edit().putBoolean("use_pc_layout_$selectedLangId", isChecked).apply()
        }
    }

    private fun setupCandidateHeightControl() {
        val seekBar = findViewById<SeekBar>(R.id.seekbar_candidate_height)
        val tvVal = findViewById<TextView>(R.id.tv_candidate_height_val)
        val prefs = getSharedPreferences("implus_prefs", Context.MODE_PRIVATE)
        
        val saved = prefs.getInt("candidate_height", 48)
        seekBar.progress = saved
        tvVal.text = "${saved}dp"

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val real = if (progress < 30) 30 else progress
                tvVal.text = "${real}dp"
                prefs.edit().putInt("candidate_height", real).apply()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
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
