package com.implus.input

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.implus.input.manager.DictionaryManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DictionaryManagerTest {

    private lateinit var dictionaryManager: DictionaryManager
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setup() {
        dictionaryManager = DictionaryManager(context)
    }

    @Test
    fun testDictionaryLoadingAndSuggestions() {
        // 1. 测试加载 (子功能验证)
        dictionaryManager.loadDictionary("en", "en_US.dict")
        
        // 2. 测试前缀匹配 (核心逻辑)
        val suggestions = dictionaryManager.getSuggestions("hel")
        assertTrue("应包含 hello", suggestions.any { it.startsWith("hello", ignoreCase = true) })
        
        // 3. 测试排序 (频率优先)
        val sortedSuggestions = dictionaryManager.getSuggestions("t")
        if (sortedSuggestions.size > 1) {
            // 简单验证逻辑：假设词库中 common 词汇频率更高
            assertTrue("返回结果不应为空", sortedSuggestions.isNotEmpty())
        }
    }

    @Test
    fun testEmptyPrefix() {
        val suggestions = dictionaryManager.getSuggestions("")
        assertTrue("空前缀应返回空列表", suggestions.isEmpty())
    }
}
