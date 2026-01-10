package com.implus.input.manager

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "implus_prefs"
        
        const val KEY_CURRENT_LANG = "current_lang"
        const val KEY_USE_PC_LAYOUT_PREFIX = "use_pc_layout_"
        const val KEY_HEIGHT_PERCENT = "height_percent"
        const val KEY_CANDIDATE_HEIGHT = "candidate_height"
        const val KEY_SWIPE_THRESHOLD = "swipe_threshold"
        const val KEY_H_SPACING = "horizontal_spacing"
        const val KEY_V_SPACING = "vertical_spacing"
        const val KEY_VIBRATION_ENABLED = "vibration_enabled"
        const val KEY_VIBRATION_STRENGTH = "vibration_strength"
        const val KEY_CLOSE_OUTSIDE = "close_outside"
        
        // Appearance & Animation Keys
        const val KEY_KEY_RADIUS = "key_radius"
        const val KEY_FUNC_KEY_RADIUS = "func_key_radius"
        const val KEY_SHADOW_OFFSET = "shadow_offset"
        const val KEY_SHADOW_ALPHA = "shadow_alpha"
        const val KEY_ANIM_DURATION = "anim_duration"
        const val KEY_RIPPLE_DURATION = "ripple_duration"
        const val KEY_CANDIDATE_TEXT_SIZE = "candidate_text_size"
        const val KEY_CANDIDATE_PADDING = "candidate_padding"

        const val DEFAULT_LANG = "en"
        const val DEFAULT_HEIGHT_PERCENT = 35
        const val DEFAULT_CANDIDATE_HEIGHT = 48
        const val DEFAULT_SWIPE_THRESHOLD = 50
        const val DEFAULT_SPACING = 6
        const val DEFAULT_VIBRATION_STRENGTH = 30
        
        const val DEFAULT_KEY_RADIUS = 24
        const val DEFAULT_FUNC_KEY_RADIUS = 12
        const val DEFAULT_SHADOW_OFFSET = 3
        const val DEFAULT_SHADOW_ALPHA = 30
        const val DEFAULT_ANIM_DURATION = 200
        const val DEFAULT_RIPPLE_DURATION = 350
        const val DEFAULT_CANDIDATE_TEXT_SIZE = 18
        const val DEFAULT_CANDIDATE_PADDING = 32
    }

    fun getString(key: String, default: String): String = prefs.getString(key, default) ?: default
    fun getInt(key: String, default: Int): Int = prefs.getInt(key, default)
    fun getBoolean(key: String, default: Boolean): Boolean = prefs.getBoolean(key, default)

    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
    }

    fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    fun putInt(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
    }

    fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    fun usePcLayout(langId: String): Boolean = getBoolean(KEY_USE_PC_LAYOUT_PREFIX + langId, true)
    
    val currentLangId: String get() = getString(KEY_CURRENT_LANG, DEFAULT_LANG)
    val heightPercent: Int get() = getInt(KEY_HEIGHT_PERCENT, DEFAULT_HEIGHT_PERCENT)
    val candidateHeight: Int get() = getInt(KEY_CANDIDATE_HEIGHT, DEFAULT_CANDIDATE_HEIGHT)
    val swipeThreshold: Int get() = getInt(KEY_SWIPE_THRESHOLD, DEFAULT_SWIPE_THRESHOLD)
    val horizontalSpacing: Int get() = getInt(KEY_H_SPACING, DEFAULT_SPACING)
    val verticalSpacing: Int get() = getInt(KEY_V_SPACING, DEFAULT_SPACING)
    val vibrationEnabled: Boolean get() = getBoolean(KEY_VIBRATION_ENABLED, true)
    val vibrationStrength: Int get() = getInt(KEY_VIBRATION_STRENGTH, DEFAULT_VIBRATION_STRENGTH)
    val closeOutside: Boolean get() = getBoolean(KEY_CLOSE_OUTSIDE, false)
    
    val keyRadius: Float get() = getInt(KEY_KEY_RADIUS, DEFAULT_KEY_RADIUS).toFloat()
    val funcKeyRadius: Float get() = getInt(KEY_FUNC_KEY_RADIUS, DEFAULT_FUNC_KEY_RADIUS).toFloat()
    val shadowOffset: Float get() = getInt(KEY_SHADOW_OFFSET, DEFAULT_SHADOW_OFFSET).toFloat()
    val shadowAlpha: Int get() = getInt(KEY_SHADOW_ALPHA, DEFAULT_SHADOW_ALPHA)
    val animDuration: Long get() = getInt(KEY_ANIM_DURATION, DEFAULT_ANIM_DURATION).toLong()
    val rippleDuration: Long get() = getInt(KEY_RIPPLE_DURATION, DEFAULT_RIPPLE_DURATION).toLong()
    val candidateTextSize: Float get() = getInt(KEY_CANDIDATE_TEXT_SIZE, DEFAULT_CANDIDATE_TEXT_SIZE).toFloat()
    val candidatePadding: Int get() = getInt(KEY_CANDIDATE_PADDING, DEFAULT_CANDIDATE_PADDING)
}
