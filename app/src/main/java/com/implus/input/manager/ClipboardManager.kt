package com.implus.input.manager

import android.content.ClipboardManager
import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object ClipboardHistoryManager {
    private const val PREFS_NAME = "clipboard_history"
    private const val KEY_HISTORY = "history"
    private const val MAX_ITEMS = 20
    private val gson = Gson()

    fun getHistory(context: Context): List<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_HISTORY, "[]")
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    fun add(context: Context, text: String) {
        if (text.isBlank()) return
        val list = getHistory(context).toMutableList()
        list.remove(text) // Remove duplicate if exists
        list.add(0, text) // Add to top
        if (list.size > MAX_ITEMS) {
            list.removeAt(list.size - 1)
        }
        
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_HISTORY, gson.toJson(list)).apply()
    }
    
    fun clear(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
    }
}
