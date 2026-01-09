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
        setupSwipeThresholdControl()
        setupCloseOutside()
        setupLanguageSettings()
    }

    private fun setupSwipeThresholdControl() {
        val seekBar = findViewById<SeekBar>(R.id.seekbar_swipe_threshold)
        val tvVal = findViewById<TextView>(R.id.tv_swipe_threshold_val)
        val prefs = getSharedPreferences("implus_prefs", Context.MODE_PRIVATE)
        
        val saved = prefs.getInt("swipe_threshold", 50)
        seekBar.progress = saved
        tvVal.text = "${saved}px"

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val real = if (progress < 10) 10 else progress
                tvVal.text = "${real}px"
                prefs.edit().putInt("swipe_threshold", real).apply()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupLanguageSettings() {
        val spinner = findViewById<Spinner>(R.id.spinner_languages)
        val switchPc = findViewById<SwitchMaterial>(R.id.switch_pc_layout)
        val prefs = getSharedPreferences("implus_prefs", Context.MODE_PRIVATE)

        val languages = LayoutManager.getAvailableLanguages(this)
        val names = languages.map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, names)
        spinner.adapter = adapter

        // Load current selected language from prefs
        val currentLangId = prefs.getString("current_lang", "en") ?: "en"
        val currentIndex = languages.indexOfFirst { it.id == currentLangId }.coerceAtLeast(0)
        spinner.setSelection(currentIndex)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selected = languages[position]
                // Save global current language
                prefs.edit().putString("current_lang", selected.id).apply()
                
                // Update switch state to reflect THIS language's preference
                val isPcEnabled = prefs.getBoolean("use_pc_layout_${selected.id}", true)
                // Temporarily remove listener to avoid triggering it
                switchPc.setOnCheckedChangeListener(null)
                switchPc.isChecked = isPcEnabled
                switchPc.setOnCheckedChangeListener { _, isChecked ->
                    prefs.edit().putBoolean("use_pc_layout_${selected.id}", isChecked).apply()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        
        // Initial listener setup handled by onItemSelected
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
