package com.implus.input

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial
import com.implus.input.layout.*
import com.implus.input.manager.SettingsManager

class SettingsActivity : AppCompatActivity() {

    private val settings by lazy { SettingsManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        setupHeightControl()
        setupCandidateHeightControl()
        setupAppearanceControl()
        setupSpacingControl()
        setupSwipeThresholdControl()
        setupAnimationControl()
        setupLanguageSettings()
        setupFeedbackControl()
        setupCloseOutside()
    }

    private fun setupAppearanceControl() {
        // Key Radius
        val seekRadius = findViewById<SeekBar>(R.id.seekbar_key_radius)
        val tvRadius = findViewById<TextView>(R.id.tv_key_radius_val)
        val savedRadius = settings.keyRadius.toInt()
        seekRadius.progress = savedRadius
        tvRadius.text = "$savedRadius"
        seekRadius.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvRadius.text = "$progress"
                settings.putInt(SettingsManager.KEY_KEY_RADIUS, progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Shadow Alpha
        val seekAlpha = findViewById<SeekBar>(R.id.seekbar_shadow_alpha)
        val tvAlpha = findViewById<TextView>(R.id.tv_shadow_alpha_val)
        val savedAlpha = settings.shadowAlpha
        seekAlpha.progress = savedAlpha
        tvAlpha.text = "$savedAlpha"
        seekAlpha.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvAlpha.text = "$progress"
                settings.putInt(SettingsManager.KEY_SHADOW_ALPHA, progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupAnimationControl() {
        // Anim Duration
        val seekAnim = findViewById<SeekBar>(R.id.seekbar_anim_duration)
        val tvAnim = findViewById<TextView>(R.id.tv_anim_duration_val)
        val savedAnim = settings.animDuration.toInt()
        seekAnim.progress = savedAnim
        tvAnim.text = "${savedAnim}ms"
        seekAnim.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val real = if (progress < 10) 10 else progress
                tvAnim.text = "${real}ms"
                settings.putInt(SettingsManager.KEY_ANIM_DURATION, real)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupFeedbackControl() {
        val switchVib = findViewById<SwitchMaterial>(R.id.switch_vibration)
        val seekVib = findViewById<SeekBar>(R.id.seekbar_vibration_strength)
        val tvVibVal = findViewById<TextView>(R.id.tv_vibration_strength_val)
        val containerVib = findViewById<View>(R.id.container_vibration_strength)

        val vibEnabled = settings.vibrationEnabled
        switchVib.isChecked = vibEnabled
        containerVib.visibility = if (vibEnabled) View.VISIBLE else View.GONE

        switchVib.setOnCheckedChangeListener { _, isChecked ->
            settings.putBoolean(SettingsManager.KEY_VIBRATION_ENABLED, isChecked)
            containerVib.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        val vibStrength = settings.vibrationStrength
        seekVib.progress = vibStrength
        tvVibVal.text = "${vibStrength}ms"

        seekVib.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvVibVal.text = "${progress}ms"
                settings.putInt(SettingsManager.KEY_VIBRATION_STRENGTH, progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupSpacingControl() {
        // Horizontal Spacing
        val seekBarH = findViewById<SeekBar>(R.id.seekbar_h_spacing)
        val tvValH = findViewById<TextView>(R.id.tv_h_spacing_val)
        val savedH = settings.horizontalSpacing
        seekBarH.progress = savedH
        tvValH.text = "${savedH}px"
        seekBarH.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvValH.text = "${progress}px"
                settings.putInt(SettingsManager.KEY_H_SPACING, progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Vertical Spacing
        val seekBarV = findViewById<SeekBar>(R.id.seekbar_v_spacing)
        val tvValV = findViewById<TextView>(R.id.tv_v_spacing_val)
        val savedV = settings.verticalSpacing
        seekBarV.progress = savedV
        tvValV.text = "${savedV}px"
        seekBarV.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvValV.text = "${progress}px"
                settings.putInt(SettingsManager.KEY_V_SPACING, progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupSwipeThresholdControl() {
        val seekBar = findViewById<SeekBar>(R.id.seekbar_swipe_threshold)
        val tvVal = findViewById<TextView>(R.id.tv_swipe_threshold_val)
        
        val saved = settings.swipeThreshold
        seekBar.progress = saved
        tvVal.text = "${saved}px"

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val real = if (progress < 10) 10 else progress
                tvVal.text = "${real}px"
                settings.putInt(SettingsManager.KEY_SWIPE_THRESHOLD, real)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupLanguageSettings() {
        val spinner = findViewById<Spinner>(R.id.spinner_languages)
        val switchPc = findViewById<SwitchMaterial>(R.id.switch_pc_layout)

        val languages = LayoutManager.getAvailableLanguages(this)
        val names = languages.map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, names)
        spinner.adapter = adapter

        // Load current selected language
        val currentLangId = settings.currentLangId
        val currentIndex = languages.indexOfFirst { it.id == currentLangId }.coerceAtLeast(0)
        spinner.setSelection(currentIndex)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selected = languages[position]
                // Save global current language
                settings.putString(SettingsManager.KEY_CURRENT_LANG, selected.id)
                
                // Update switch state to reflect THIS language's preference
                val isPcEnabled = settings.usePcLayout(selected.id)
                // Temporarily remove listener to avoid triggering it
                switchPc.setOnCheckedChangeListener(null)
                switchPc.isChecked = isPcEnabled
                switchPc.setOnCheckedChangeListener { _, isChecked ->
                    settings.putBoolean(SettingsManager.KEY_USE_PC_LAYOUT_PREFIX + selected.id, isChecked)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupCandidateHeightControl() {
        val seekBar = findViewById<SeekBar>(R.id.seekbar_candidate_height)
        val tvVal = findViewById<TextView>(R.id.tv_candidate_height_val)
        
        val saved = settings.candidateHeight
        seekBar.progress = saved
        tvVal.text = "${saved}dp"

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val real = if (progress < 30) 30 else progress
                tvVal.text = "${real}dp"
                settings.putInt(SettingsManager.KEY_CANDIDATE_HEIGHT, real)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupHeightControl() {
        val seekBar = findViewById<SeekBar>(R.id.seekbar_height)
        val tvVal = findViewById<TextView>(R.id.tv_height_val)
        
        val savedPercent = settings.heightPercent
        seekBar.progress = savedPercent
        tvVal.text = "$savedPercent%"

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val realProgress = if (progress < 20) 20 else progress
                tvVal.text = "$realProgress%"
                settings.putInt(SettingsManager.KEY_HEIGHT_PERCENT, realProgress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupCloseOutside() {
        val switch = findViewById<SwitchMaterial>(R.id.switch_close_outside)
        
        switch.isChecked = settings.closeOutside
        
        switch.setOnCheckedChangeListener { _, isChecked ->
            settings.putBoolean(SettingsManager.KEY_CLOSE_OUTSIDE, isChecked)
        }
    }
}
