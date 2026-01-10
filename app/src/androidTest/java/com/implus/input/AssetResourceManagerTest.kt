package com.implus.input

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.implus.input.manager.AssetResourceManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AssetResourceManagerTest {

    private lateinit var assetRes: AssetResourceManager
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setup() {
        assetRes = AssetResourceManager(context)
    }

    @Test
    fun testGetStringWithFallback() {
        // 验证基本加载 (en.json 肯定存在)
        val welcome = assetRes.getString("welcome_title")
        assertNotEquals("welcome_title", welcome)
        
        // 验证不存在的 key 返回 key 本身
        val missing = assetRes.getString("non_existent_key_12345")
        assertEquals("non_existent_key_12345", missing)
    }

    @Test
    fun testGetColorWithDefault() {
        // 验证颜色加载
        val bg = assetRes.getColor("keyboard_background", -1)
        assertNotEquals(-1, bg)
        
        // 验证不存在的颜色返回默认值
        val defaultColor = 0xFF123456.toInt()
        val missing = assetRes.getColor("non_existent_color", defaultColor)
        assertEquals(defaultColor, missing)
    }

    @Test
    fun testRefresh() {
        // 模拟刷新，不应崩溃
        assetRes.refresh()
        assertNotNull(assetRes.getString("welcome_title"))
    }
}
