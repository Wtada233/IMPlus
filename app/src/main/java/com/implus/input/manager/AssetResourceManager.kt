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
        // 1. 加载语言：尝试匹配当前语言代码，找不到则回退到 en
        val locale = context.resources.configuration.locales[0]
        val langCode = locale.language
        val countryCode = locale.country
        
        var langFile = "i18n/en.json" // 默认回退
        val availableI18n = try { context.assets.list("i18n") ?: emptyArray() } catch (e: Exception) { emptyArray() }
        
        // 优先级：语言_国家 (zh_CN.json) > 语言 (zh.json) > en.json
        val fullMatch = "${langCode}_$countryCode.json"
        val langMatch = "$langCode.json"
        
        when {
            availableI18n.contains(fullMatch) -> langFile = "i18n/$fullMatch"
            availableI18n.contains(langMatch) -> langFile = "i18n/$langMatch"
        }
        
        strings = loadJson(langFile)

        // 2. 加载主题
        val isDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val themeFile = if (isDark) "themes/dark.json" else "themes/light.json"
        colors = loadJson(themeFile)
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