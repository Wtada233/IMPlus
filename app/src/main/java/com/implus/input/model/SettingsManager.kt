package com.implus.input.model

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("implus_settings", Context.MODE_PRIVATE)

    var keyboardHeight: Int
        get() = prefs.getInt("keyboard_height", 260)
        set(value) = prefs.edit().putInt("keyboard_height", value).apply()

    var hapticEnabled: Boolean
        get() = prefs.getBoolean("haptic_enabled", true)
        set(value) = prefs.edit().putBoolean("haptic_enabled", value).apply()
}
