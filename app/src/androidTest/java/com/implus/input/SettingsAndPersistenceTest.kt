package com.implus.input

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.implus.input.manager.ClipboardHistoryManager
import com.implus.input.manager.SettingsManager
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsAndPersistenceTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun testSettingsPersistence() {
        val settings = SettingsManager(context)
        val testValue = 42
        
        settings.putInt(SettingsManager.KEY_HEIGHT_PERCENT, testValue)
        
        // 重新创建实例模拟重启
        val newSettings = SettingsManager(context)
        assertEquals("设置值应被持久化", testValue, newSettings.heightPercent)
    }

    @Test
    fun testClipboardHistory() {
        ClipboardHistoryManager.clear(context)
        
        val testText1 = "Hello Clipboard"
        val testText2 = "Second Item"
        
        ClipboardHistoryManager.add(context, testText1)
        ClipboardHistoryManager.add(context, testText2)
        
        val history = ClipboardHistoryManager.getHistory(context)
        
        assertEquals("剪贴板应包含 2 条记录", 2, history.size)
        assertEquals("最近添加的应在顶部", testText2, history[0])
        
        // 测试排重
        ClipboardHistoryManager.add(context, testText1)
        val newHistory = ClipboardHistoryManager.getHistory(context)
        assertEquals("再次添加相同内容，总数不应增加", 2, newHistory.size)
        assertEquals("重复内容应被移动到顶部", testText1, newHistory[0])
    }
}
