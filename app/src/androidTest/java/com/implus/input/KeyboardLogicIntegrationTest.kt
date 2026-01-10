package com.implus.input

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.implus.input.layout.*
import com.implus.input.manager.AssetResourceManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KeyboardLogicIntegrationTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var assetRes: AssetResourceManager

    @Before
    fun setup() {
        assetRes = AssetResourceManager(context)
    }

    /**
     * 大功能测试：布局与资源加载集成
     */
    @Test
    fun testLayoutAndResourceIntegration() {
        try {
            // 尝试加载英文布局
            val layout = LayoutManager.loadLayout(context, "en", "mobile_layout.json")
            assertNotNull("布局文件应成功加载", layout)
            assertTrue("布局应包含页面", layout!!.pages.isNotEmpty())

            // 验证资源 Key 是否能正确映射
            val welcomeText = assetRes.getString("welcome_title")
            assertNotEquals("资源不应返回 Key 本身 (可能未加载)", "welcome_title", welcomeText)
            
        } catch (e: Throwable) {
            // 如果上述大功能失败，执行“可选/诊断”子测试
            runDiagnosticSubTests()
            throw e
        }
    }

    /**
     * 子功能诊断测试：仅在集成测试失败时提供详细报告
     */
    private fun runDiagnosticSubTests() {
        println("--- DIAGNOSTIC START ---")
        
        // 1. 检查 Asset 目录是否存在
        val assets = context.assets.list("languages/en")
        println("Assets in en: ${assets?.joinToString() ?: "none"}")
        
        // 2. 检查 I18n 文件
        val i18n = context.assets.list("i18n")
        println("I18n files: ${i18n?.joinToString() ?: "none"}")
        
        // 3. 验证 JSON 解析器
        try {
            val config = LayoutManager.loadLanguageConfig(context, "en")
            println("Language config id: ${config?.id}")
        } catch (jsonErr: Exception) {
            println("JSON Parsing Error: ${jsonErr.message}")
        }
        
        println("--- DIAGNOSTIC END ---")
    }

    /**
     * 功能测试：Shift 状态覆盖逻辑 (Overrides)
     */
    @Test
    fun testKeyOverrideLogic() {
        // 创建一个模拟按键
        val key = KeyboardKey(
            id = "test_key",
            label = "a",
            overrides = mapOf(
                "shift" to KeyOverride(label = "A")
            )
        )

        // 模拟 Service 状态
        val activeStates = mutableMapOf("shift" to true)
        
        var effectiveLabel = key.label
        key.overrides?.forEach { (stateId, override) ->
            if (activeStates[stateId] == true) {
                override.label?.let { effectiveLabel = it }
            }
        }

        assertEquals("在 Shift 激活时，Label 应变为大写", "A", effectiveLabel)
    }
}
