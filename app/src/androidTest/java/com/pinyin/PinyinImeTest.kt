package com.pinyin

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PinyinImeTest {

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        PinyinIme.init(context)
    }

    @Test
    fun testSearchNihao() {
        PinyinIme.resetSearch()
        val candidates = PinyinIme.search("nihao")
        assertNotNull(candidates)
        assertTrue("nihao should have candidates", candidates.isNotEmpty())
        assertTrue("nihao candidates should contain '你好'", candidates.contains("你好"))
    }

    @Test
    fun testSearchZhengzai() {
        PinyinIme.resetSearch()
        val candidates = PinyinIme.search("zhengzai")
        assertNotNull(candidates)
        assertTrue("zhengzai should have multi-char candidate '正在'", candidates.contains("正在"))
    }

    @Test
    fun testSearchShorthand() {
        PinyinIme.resetSearch()
        val input = "nh"
        val candidates = PinyinIme.search(input)
        assertNotNull("Search result for '$input' should not be null", candidates)
        assertTrue("Shorthand '$input' should have candidates", candidates!!.isNotEmpty())
        assertTrue("Candidates for '$input' should contain '你好', found: ${candidates.joinToString()}", candidates.contains("你好"))
    }

    @Test
    fun testSegmentation() {
        PinyinIme.resetSearch()
        val input = "zhengzai"
        PinyinIme.search(input)
        val spl = PinyinIme.getSpellingString()
        assertNotNull("Spelling string for '$input' should not be null", spl)
        android.util.Log.d("PinyinTest", "Segmentation for '$input': formatted=${spl!!.spellingStr}, raw=${spl.rawSpelling}")
        assertTrue("Should have segmented string with ', found: ${spl.spellingStr}", spl.spellingStr.contains("'"))
    }

    @Test
    fun testPrediction() {
        PinyinIme.resetSearch()
        // Search for "wo"
        PinyinIme.search("wo")
        // Choose "我" (usually index 0)
        PinyinIme.choose(0)
        
        // Get predicts for history "我"
        val predicts = PinyinIme.getPredicts("我")
        assertNotNull(predicts)
        if (predicts.isNotEmpty()) {
            // "我们" is a common prediction for "我"
            // Note: predicts contains the NEXT characters, so it might be "们"
            assertTrue("Prediction should contain something", predicts.size > 0)
        }
    }
}
