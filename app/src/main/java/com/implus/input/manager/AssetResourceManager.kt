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

    init {
        refresh()
    }

    /**
     * 根据当前系统语言和深浅色模式刷新缓存
     */
    fun refresh() {
        // 1. 加载语言：先加载 en.json 作为兜底，再叠加当前语言
        val baseStrings = loadJson("i18n/en.json")
        
        val locale = context.resources.configuration.locales[0]
        val langCode = locale.language
        val countryCode = locale.country
        
        val availableI18n = try { context.assets.list("i18n") ?: emptyArray() } catch (e: Exception) { emptyArray() }
        
        val fullMatch = "${langCode}_$countryCode.json"
        val langMatch = "$langCode.json"
        
        val currentStrings = when {
            availableI18n.contains(fullMatch) -> loadJson("i18n/$fullMatch")
            availableI18n.contains(langMatch) -> loadJson("i18n/$langMatch")
            else -> emptyMap()
        }
        
        // 合并资源，currentStrings 覆盖 baseStrings
        strings = baseStrings.toMutableMap().apply { putAll(currentStrings) }

        // 2. 加载主题：默认加载 light.json 兜底
        val baseColors = loadJson("themes/light.json")
        val isDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        
        val currentColors = if (isDark) loadJson("themes/dark.json") else emptyMap()
        
        colors = baseColors.toMutableMap().apply { putAll(currentColors) }
    }

    private fun loadJson(path: String): Map<String, String> {
        return try {
            val reader = InputStreamReader(context.assets.open(path))
            val type = object : TypeToken<Map<String, String>>() {}.type
            val result: Map<String, String> = gson.fromJson(reader, type)
            reader.close()
            result
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun getString(key: String, vararg args: Any): String {
        val template = strings[key] ?: key
        return try {
            String.format(template, *args)
        } catch (e: Exception) {
            template
        }
    }

    fun getColor(key: String, default: Int): Int {
        val hex = colors[key] ?: return default
        return try {
            android.graphics.Color.parseColor(hex)
        } catch (e: Exception) {
            default
        }
    }
}