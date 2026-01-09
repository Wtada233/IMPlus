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
                val config = loadLanguageConfig(context, langDir)
                if (config != null) {
                    languages.add(config)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error listing languages", e)
        }
        return languages
    }

    fun loadLanguageConfig(context: Context, langId: String): LanguageConfig? {
        val configPath = "$LANGUAGES_DIR/$langId/config.json"
        return try {
            val inputStream = context.assets.open(configPath)
            val reader = InputStreamReader(inputStream)
            val config = gson.fromJson(reader, LanguageConfig::class.java)
            reader.close()
            config
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load config for $langId: ${e.message}")
            null
        }
    }

    fun loadLayout(context: Context, langId: String, fileName: String): KeyboardLayout? {
        val layoutPath = "$LANGUAGES_DIR/$langId/$fileName"
        Log.d(TAG, "Attempting to load layout: $layoutPath")
        return try {
            val inputStream = context.assets.open(layoutPath)
            val reader = InputStreamReader(inputStream)
            val layout = gson.fromJson(reader, KeyboardLayout::class.java)
            reader.close()
            if (layout == null) {
                Log.e(TAG, "Parsed layout is null for $layoutPath")
                return null
            }
            Log.d(TAG, "Successfully loaded layout: ${layout.name} from $layoutPath")
            layout
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load layout $layoutPath: ${e.message}", e)
            null
        }
    }
}