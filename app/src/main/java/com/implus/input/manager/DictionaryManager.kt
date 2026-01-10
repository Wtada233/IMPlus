package com.implus.input.manager

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader

class DictionaryManager(private val context: Context) {
    data class Word(val text: String, val freq: Int)

    companion object {
        private const val MAX_WORDS_LIMIT = 320000
    }

    private val loadLock = Any()
    @Volatile private var words = listOf<Word>()
    @Volatile private var currentDictPath: String? = null

    fun loadDictionary(langId: String, fileName: String) {
        val path = "languages/$langId/$fileName"
        
        synchronized(loadLock) {
            if (path == currentDictPath) return

            try {
                val inputStream = context.assets.open(path)
                val reader = BufferedReader(InputStreamReader(inputStream))
                val loadedWords = mutableListOf<Word>()
                var count = 0
                val maxWords = MAX_WORDS_LIMIT // 提高上限以支持更大词库
                
                reader.forEachLine { line ->
                    if (count >= maxWords) return@forEachLine
                    val parts = line.split("\t", "=", " ", limit = 2)
                    if (parts.isNotEmpty()) {
                        val word = parts[0].trim()
                        val freq = if (parts.size > 1) parts[1].trim().toIntOrNull() ?: 0 else 0
                        if (word.isNotEmpty()) {
                            loadedWords.add(Word(word, freq))
                            count++
                        }
                    }
                }
                // 必须按字母顺序排序，以便进行二分查找前缀
                // 使用 ROOT Locale 确保排序与后续搜索的一致性
                words = loadedWords.sortedBy { it.text.lowercase(java.util.Locale.ROOT) }
                currentDictPath = path
                Log.d("DictionaryManager", "Loaded ${words.size} words with frequencies from $path")
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                Log.e("DictionaryManager", "Failed to load dictionary: $path", e)
                words = emptyList()
                currentDictPath = null
            } catch (e: OutOfMemoryError) {
                Log.e("DictionaryManager", "OOM loading dictionary: $path", e)
                words = emptyList()
                currentDictPath = null
            }
        }
    }

    fun getSuggestions(prefix: String, limit: Int = 10): List<String> {
        if (prefix.isBlank() || words.isEmpty()) {
            return emptyList()
        }
        val lowerPrefix = prefix.lowercase(java.util.Locale.ROOT)
        
        val firstMatchIndex = findFirstMatchIndex(lowerPrefix)
        if (firstMatchIndex == -1) {
            return emptyList()
        }

        return collectAndSortMatches(lowerPrefix, firstMatchIndex, limit)
    }

    private fun findFirstMatchIndex(lowerPrefix: String): Int {
        var low = 0
        var high = words.size - 1
        var firstMatchIndex = -1
        
        while (low <= high) {
            val mid = (low + high) / 2
            val midWord = words[mid].text.lowercase(java.util.Locale.ROOT)
            if (midWord.startsWith(lowerPrefix)) {
                firstMatchIndex = mid
                high = mid - 1
            } else if (midWord < lowerPrefix) {
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return firstMatchIndex
    }

    private fun collectAndSortMatches(lowerPrefix: String, startIndex: Int, limit: Int): List<String> {
        val matches = mutableListOf<Word>()
        for (i in startIndex until words.size) {
            val w = words[i]
            if (w.text.lowercase(java.util.Locale.ROOT).startsWith(lowerPrefix)) {
                matches.add(w)
            } else {
                break
            }
        }

        return matches.sortedByDescending { it.freq }
            .take(limit)
            .map { it.text }
    }
    
    fun clear() {
        synchronized(loadLock) {
            words = emptyList()
            currentDictPath = null
        }
    }
}

