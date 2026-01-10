package com.implus.input.manager

import android.content.Context
import android.content.res.Configuration
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader
import java.util.*

class AssetResourceManager(private val context: Context) {
    private val gson = Gson()
    private var strings: Map<String, String> = emptyMap()
    private var colors: Map<String, String> = emptyMap()
    private val jsonCache = mutableMapOf<String, Map<String, String>>()

    init {
        refresh()
    }

    /**
     * 根据当前系统语言和深浅色模式刷新缓存
     */
    fun refresh() {
        // 清理缓存以允许重新加载（如果文件可能变动），或者根据需要保留
        // 这里我们选择保留缓存，仅在 refresh 时重新组合逻辑
        
        // 1. 加载语言：先加载 en.json 作为兜底，再叠加当前语言
        val baseStrings = getOrLoadJson("i18n/en.json")
        
        val locale = context.resources.configuration.locales[0]
        val langCode = locale.language
        val countryCode = locale.country
        
        val availableI18n = try { 
            context.assets.list("i18n") ?: emptyArray() 
        } catch (e: java.io.IOException) {
            android.util.Log.e("AssetResourceManager", "Failed to list assets", e)
            emptyArray()
        }
        
        val fullMatch = "${langCode}_$countryCode.json"
        val langMatch = "$langCode.json"
        
        val currentStrings = when {
            availableI18n.contains(fullMatch) -> getOrLoadJson("i18n/$fullMatch")
            availableI18n.contains(langMatch) -> getOrLoadJson("i18n/$langMatch")
            else -> emptyMap()
        }
        
        // 合并资源，currentStrings 覆盖 baseStrings
        strings = baseStrings.toMutableMap().apply { putAll(currentStrings) }

        // 2. 加载主题：默认加载 light.json 兜底
        val baseColors = getOrLoadJson("themes/light.json")
        val isDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        
        val currentColors = if (isDark) getOrLoadJson("themes/dark.json") else emptyMap()
        
        colors = baseColors.toMutableMap().apply { putAll(currentColors) }
    }

    private fun getOrLoadJson(path: String): Map<String, String> {
        return jsonCache.getOrPut(path) { loadJson(path) }
    }

    private fun loadJson(path: String): Map<String, String> {
        return try {
            val inputStream = context.assets.open(path)
            val reader = InputStreamReader(inputStream)
            val type = object : TypeToken<Map<String, String>>() {}.type
            val result: Map<String, String>? = gson.fromJson(reader, type)
            reader.close()
            result ?: emptyMap()
        } catch (e: java.io.IOException) {
            android.util.Log.e("AssetResourceManager", "IO error loading $path", e)
            emptyMap()
        } catch (e: com.google.gson.JsonSyntaxException) {
            android.util.Log.e("AssetResourceManager", "JSON syntax error in $path", e)
            emptyMap()
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            // Fallback for other unexpected errors
            android.util.Log.e("AssetResourceManager", "Unknown error loading $path", e)
            emptyMap()
        }
    }

    fun getString(key: String, vararg args: Any): String {
        val template = strings[key] ?: key
        return try {
            String.format(template, *args)
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            android.util.Log.e("AssetResourceManager", "Format error for key $key", e)
            template
        }
    }

    fun getColor(key: String, default: Int): Int {
        val hex = colors[key] ?: return default
        return try {
            android.graphics.Color.parseColor(hex)
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            android.util.Log.e("AssetResourceManager", "Color parse error: $hex", e)
            default
        }
    }
}


