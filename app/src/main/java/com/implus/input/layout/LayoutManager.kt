package com.implus.input.layout

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import java.io.InputStreamReader

object LayoutManager {
    private val gson = Gson()
    private const val TAG = "LayoutManager"
    private const val LANGUAGES_DIR = "languages"

    fun getAvailableLanguages(context: Context): List<LanguageConfig> {
        val languages = mutableListOf<LanguageConfig>()
        try {
            val langDirs = context.assets.list(LANGUAGES_DIR) ?: emptyArray()
            for (langDir in langDirs) {
                loadLanguageConfig(context, langDir)?.let { languages.add(it) }
            }
        } catch (e: java.io.IOException) {
            Log.e(TAG, "Error listing languages", e)
        }
        return languages
    }

    fun loadLanguageConfig(context: Context, langId: String): LanguageConfig? {
        val configPath = "$LANGUAGES_DIR/$langId/config.json"
        return try {
            context.assets.open(configPath).use { inputStream ->
                InputStreamReader(inputStream).use { reader ->
                    gson.fromJson(reader, LanguageConfig::class.java)
                }
            }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Log.e(TAG, "Failed to load config for $langId", e)
            null
        }
    }

    fun loadLayout(context: Context, langId: String, fileName: String): KeyboardLayout? {
        val layoutPath = "$LANGUAGES_DIR/$langId/$fileName"
        Log.d(TAG, "Attempting to load layout: $layoutPath")
        return try {
            context.assets.open(layoutPath).use { inputStream ->
                InputStreamReader(inputStream).use { reader ->
                    gson.fromJson(reader, KeyboardLayout::class.java).also {
                        if (it == null) Log.e(TAG, "Parsed layout is null for $layoutPath")
                        else Log.d(TAG, "Successfully loaded layout: ${it.name}")
                    }
                }
            }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Log.e(TAG, "Failed to load layout $layoutPath", e)
            null
        }
    }
}


