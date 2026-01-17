package com.implus.input

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.implus.input.layout.LayoutManager
import com.implus.input.manager.SettingsManager
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LanguageIntegrationTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun testAvailableLanguages() {
        val languages = LayoutManager.getAvailableLanguages(context)
        assertTrue("Should find at least 'en' and 'zh'", languages.size >= 2)
        assertTrue(languages.any { it.id == "en" })
        assertTrue(languages.any { it.id == "zh" })
    }

    @Test
    fun testLanguageRotationLogic() {
        val settings = SettingsManager(context)
        val languages = LayoutManager.getAvailableLanguages(context)
        
        // Ensure we have languages to rotate
        if (languages.size < 2) return

        val initialLang = settings.currentLangId
        val initialIndex = languages.indexOfFirst { it.id == initialLang }.coerceAtLeast(0)
        
        // Simulate rotation logic (like in rotateLanguage())
        val nextIndex = (initialIndex + 1) % languages.size
        val nextLang = languages[nextIndex]
        
        settings.putString(SettingsManager.KEY_CURRENT_LANG, nextLang.id)
        
        assertEquals("Language should have changed in settings", nextLang.id, settings.currentLangId)
        assertNotEquals("New language should be different", initialLang, settings.currentLangId)
    }

    @Test
    fun testEngineSelectionDataDriven() {
        // Verify 'zh' uses 'pinyin' engine while 'en' uses 'dictionary'
        val zhConfig = LayoutManager.loadLanguageConfig(context, "zh")
        val enConfig = LayoutManager.loadLanguageConfig(context, "en")
        
        assertNotNull(zhConfig)
        assertNotNull(enConfig)
        
        assertEquals("zh should use pinyin engine", "pinyin", zhConfig?.engine)
        assertEquals("en should use dictionary engine", "dictionary", enConfig?.engine)
    }
}
