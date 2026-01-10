package com.implus.input.manager

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader

class DictionaryManager(private val context: Context) {
    private var words = listOf<String>()
    private var currentDictPath: String? = null

    fun loadDictionary(langId: String, fileName: String) {
        val path = "languages/$langId/$fileName"
        if (path == currentDictPath) return

        try {
            val inputStream = context.assets.open(path)
            val reader = BufferedReader(InputStreamReader(inputStream))
            words = reader.readLines().filter { it.isNotBlank() }.sorted()
            currentDictPath = path
            Log.d("DictionaryManager", "Loaded ${words.size} words from $path")
        } catch (e: Exception) {
            Log.e("DictionaryManager", "Failed to load dictionary: $path", e)
            words = emptyList()
            currentDictPath = null
        }
    }

    fun getSuggestions(prefix: String, limit: Int = 10): List<String> {
        if (prefix.isBlank()) return emptyList()
        val lowerPrefix = prefix.lowercase()
        
        // 简单的过滤逻辑，对于测试用的 100 词库足够快
        // 如果词库变大（如 10w 词），这里应该换成二分查找找到范围
        return words.asSequence()
            .filter { it.lowercase().startsWith(lowerPrefix) }
            .take(limit)
            .toList()
    }
    
    fun clear() {
        words = emptyList()
        currentDictPath = null
    }
}
