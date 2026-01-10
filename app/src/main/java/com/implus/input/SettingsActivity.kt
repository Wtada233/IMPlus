package com.implus.input

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial
import com.implus.input.layout.*
import com.implus.input.manager.SettingsManager
import com.implus.input.manager.AssetResourceManager

class SettingsActivity : AppCompatActivity() {

    private val settings by lazy { SettingsManager(this) }
    private val assetRes by lazy { AssetResourceManager(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        applyTexts()
        setupHeightControl()
        setupCandidateHeightControl()
        setupAppearanceControl()
        setupSpacingControl()
        setupSwipeThresholdControl()
        setupAnimationControl()
        setupAdvancedLayoutControl()
        setupLanguageSettings()
        setupFeedbackControl()
        setupCloseOutside()
    }

    private fun applyTexts() {
        assetRes.refresh()
        findViewById<TextView>(R.id.tv_settings_title)?.text = assetRes.getString("settings_title")
        findViewById<TextView>(R.id.tv_height_title)?.text = assetRes.getString("pref_height_title")
        findViewById<TextView>(R.id.tv_candidate_height_title)?.text = assetRes.getString("pref_candidate_height_title")
        findViewById<TextView>(R.id.tv_category_appearance)?.text = assetRes.getString("pref_category_appearance")
        findViewById<TextView>(R.id.tv_key_radius_title)?.text = assetRes.getString("pref_key_radius")
        findViewById<TextView>(R.id.tv_shadow_alpha_title)?.text = assetRes.getString("pref_shadow_alpha")
        findViewById<TextView>(R.id.tv_shadow_offset_title)?.text = assetRes.getString("pref_shadow_offset")
        findViewById<TextView>(R.id.tv_h_spacing_title)?.text = assetRes.getString("pref_h_spacing_title")
        findViewById<TextView>(R.id.tv_v_spacing_title)?.text = assetRes.getString("pref_v_spacing_title")
        findViewById<TextView>(R.id.tv_category_animation)?.text = assetRes.getString("pref_category_animation")
        findViewById<TextView>(R.id.tv_anim_duration_title)?.text = assetRes.getString("pref_anim_duration")
        findViewById<TextView>(R.id.tv_ripple_duration_title)?.text = assetRes.getString("pref_ripple_duration")
        findViewById<TextView>(R.id.tv_category_behavior)?.text = assetRes.getString("pref_category_behavior")
        findViewById<SwitchMaterial>(R.id.switch_close_outside)?.text = assetRes.getString("pref_close_outside")
        findViewById<TextView>(R.id.tv_category_advanced)?.text = assetRes.getString("pref_category_advanced")
        findViewById<TextView>(R.id.tv_candidate_text_size_title)?.text = assetRes.getString("pref_candidate_text_size")
        findViewById<TextView>(R.id.tv_candidate_padding_title)?.text = assetRes.getString("pref_candidate_padding")
        findViewById<TextView>(R.id.tv_category_feedback)?.text = assetRes.getString("pref_category_feedback")
        findViewById<SwitchMaterial>(R.id.switch_vibration)?.text = assetRes.getString("pref_vibration_enable")
        findViewById<TextView>(R.id.tv_vibration_strength_label)?.text = assetRes.getString("pref_vibration_strength")
        findViewById<TextView>(R.id.tv_category_language)?.text = assetRes.getString("pref_category_language")
        findViewById<SwitchMaterial>(R.id.switch_pc_layout)?.text = assetRes.getString("pref_enable_pc_layout")
        findViewById<SwitchMaterial>(R.id.switch_dict_pc)?.text = assetRes.getString("pref_dict_pc_enable")
        findViewById<SwitchMaterial>(R.id.switch_dict_mobile)?.text = assetRes.getString("pref_dict_mobile_enable")
    }

    private fun setupAppearanceControl() {
        val seekRadius = findViewById<SeekBar>(R.id.seekbar_key_radius)
        val tvRadius = findViewById<TextView>(R.id.tv_key_radius_val)
        val savedRadius = settings.keyRadius.toInt()
        seekRadius.progress = savedRadius
        tvRadius.text = assetRes.getString("unit_px", savedRadius)
        seekRadius.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvRadius.text = assetRes.getString("unit_px", progress)
                settings.putInt(SettingsManager.KEY_KEY_RADIUS, progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        val seekAlpha = findViewById<SeekBar>(R.id.seekbar_shadow_alpha)
        val tvAlpha = findViewById<TextView>(R.id.tv_shadow_alpha_val)
        val savedAlpha = settings.shadowAlpha
        seekAlpha.progress = savedAlpha
        tvAlpha.text = assetRes.getString("unit_percent", (savedAlpha * 100 / 255))
        seekAlpha.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvAlpha.text = assetRes.getString("unit_percent", (progress * 100 / 255))
                settings.putInt(SettingsManager.KEY_SHADOW_ALPHA, progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        val seekOffset = findViewById<SeekBar>(R.id.seekbar_shadow_offset)
        val tvOffset = findViewById<TextView>(R.id.tv_shadow_offset_val)
        val savedOffset = settings.shadowOffset.toInt()
        seekOffset.progress = savedOffset
        tvOffset.text = assetRes.getString("unit_px", savedOffset)
        seekOffset.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvOffset.text = assetRes.getString("unit_px", progress)
                settings.putInt(SettingsManager.KEY_SHADOW_OFFSET, progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupAnimationControl() {
        val seekAnim = findViewById<SeekBar>(R.id.seekbar_anim_duration)
        val tvAnim = findViewById<TextView>(R.id.tv_anim_duration_val)
        val savedAnim = settings.animDuration.toInt()
        seekAnim.progress = savedAnim
        tvAnim.text = assetRes.getString("unit_ms", savedAnim)
        seekAnim.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val real = if (progress < 10) 10 else progress
                tvAnim.text = assetRes.getString("unit_ms", real)
                settings.putInt(SettingsManager.KEY_ANIM_DURATION, real)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        val seekRipple = findViewById<SeekBar>(R.id.seekbar_ripple_duration)
        val tvRipple = findViewById<TextView>(R.id.tv_ripple_duration_val)
        val savedRipple = settings.rippleDuration.toInt()
        seekRipple.progress = savedRipple
        tvRipple.text = assetRes.getString("unit_ms", savedRipple)
        seekRipple.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val real = if (progress < 10) 10 else progress
                tvRipple.text = assetRes.getString("unit_ms", real)
                settings.putInt(SettingsManager.KEY_RIPPLE_DURATION, real)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupAdvancedLayoutControl() {
        val seekSize = findViewById<SeekBar>(R.id.seekbar_candidate_text_size)
        val tvSize = findViewById<TextView>(R.id.tv_candidate_text_size_val)
        val savedSize = settings.candidateTextSize.toInt()
        seekSize.progress = savedSize
        tvSize.text = assetRes.getString("unit_sp", savedSize)
        seekSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvSize.text = assetRes.getString("unit_sp", progress)
                settings.putInt(SettingsManager.KEY_CANDIDATE_TEXT_SIZE, progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        val seekPadding = findViewById<SeekBar>(R.id.seekbar_candidate_padding)
        val tvPadding = findViewById<TextView>(R.id.tv_candidate_padding_val)
        val savedPadding = settings.candidatePadding
        seekPadding.progress = savedPadding
        tvPadding.text = assetRes.getString("unit_px", savedPadding)
        seekPadding.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvPadding.text = assetRes.getString("unit_px", progress)
                settings.putInt(SettingsManager.KEY_CANDIDATE_PADDING, progress)
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
        tvVibVal.text = assetRes.getString("unit_ms", vibStrength)

        seekVib.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvVibVal.text = assetRes.getString("unit_ms", progress)
                settings.putInt(SettingsManager.KEY_VIBRATION_STRENGTH, progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupSpacingControl() {
        val seekBarH = findViewById<SeekBar>(R.id.seekbar_h_spacing)
        val tvValH = findViewById<TextView>(R.id.tv_h_spacing_val)
        val savedH = settings.horizontalSpacing
        seekBarH.progress = savedH
        tvValH.text = assetRes.getString("unit_px", savedH)
        seekBarH.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvValH.text = assetRes.getString("unit_px", progress)
                settings.putInt(SettingsManager.KEY_H_SPACING, progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        val seekBarV = findViewById<SeekBar>(R.id.seekbar_v_spacing)
        val tvValV = findViewById<TextView>(R.id.tv_v_spacing_val)
        val savedV = settings.verticalSpacing
        seekBarV.progress = savedV
        tvValV.text = assetRes.getString("unit_px", savedV)
        seekBarV.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tvValV.text = assetRes.getString("unit_px", progress)
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
        tvVal.text = assetRes.getString("unit_px", saved)

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val real = if (progress < 10) 10 else progress
                tvVal.text = assetRes.getString("unit_px", real)
                settings.putInt(SettingsManager.KEY_SWIPE_THRESHOLD, real)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setupLanguageSettings() {
        val spinner = findViewById<Spinner>(R.id.spinner_languages)
        val switchPc = findViewById<SwitchMaterial>(R.id.switch_pc_layout)
        val switchDictPc = findViewById<SwitchMaterial>(R.id.switch_dict_pc)
        val switchDictMobile = findViewById<SwitchMaterial>(R.id.switch_dict_mobile)
        
        val languages = LayoutManager.getAvailableLanguages(this)
        val names = languages.map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, names)
        spinner.adapter = adapter

        val currentLangId = settings.currentLangId
        val currentIndex = languages.indexOfFirst { it.id == currentLangId }.coerceAtLeast(0)
        spinner.setSelection(currentIndex)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selected = languages[position]
                settings.putString(SettingsManager.KEY_CURRENT_LANG, selected.id)
                
                val isPcEnabled = settings.usePcLayout(selected.id)
                switchPc.setOnCheckedChangeListener(null)
                switchPc.isChecked = isPcEnabled
                switchPc.setOnCheckedChangeListener { _, isChecked ->
                    settings.putBoolean(SettingsManager.KEY_USE_PC_LAYOUT_PREFIX + selected.id, isChecked)
                }

                updateDictSwitches(selected.id, switchDictPc, switchDictMobile)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun updateDictSwitches(langId: String, swPc: SwitchMaterial, swMobile: SwitchMaterial) {
        val isDictPcEnabled = settings.isDictEnabled(langId, true)
        swPc.setOnCheckedChangeListener(null)
        swPc.isChecked = isDictPcEnabled
        swPc.setOnCheckedChangeListener { _, isChecked ->
            settings.setDictEnabled(langId, true, isChecked)
        }

        val isDictMobileEnabled = settings.isDictEnabled(langId, false)
        swMobile.setOnCheckedChangeListener(null)
        swMobile.isChecked = isDictMobileEnabled
        swMobile.setOnCheckedChangeListener { _, isChecked ->
            settings.setDictEnabled(langId, false, isChecked)
        }
    }

    private fun setupCandidateHeightControl() {
        val seekBar = findViewById<SeekBar>(R.id.seekbar_candidate_height)
        val tvVal = findViewById<TextView>(R.id.tv_candidate_height_val)
        
        val saved = settings.candidateHeight
        seekBar.progress = saved
        tvVal.text = assetRes.getString("unit_dp", saved)

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val real = if (progress < 30) 30 else progress
                tvVal.text = assetRes.getString("unit_dp", real)
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
        tvVal.text = assetRes.getString("unit_percent", savedPercent)

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val realProgress = if (progress < 20) 20 else progress
                tvVal.text = assetRes.getString("unit_percent", realProgress)
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