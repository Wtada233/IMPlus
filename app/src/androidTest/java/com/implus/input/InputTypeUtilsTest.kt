package com.implus.input

import android.view.inputmethod.EditorInfo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.implus.input.utils.InputTypeUtils
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InputTypeUtilsTest {

    @Test
    fun testIsPasswordType() {
        val info = EditorInfo().apply {
            inputType = EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_PASSWORD
        }
        assertTrue("应识别为文本密码框", InputTypeUtils.isPasswordType(info))

        val numInfo = EditorInfo().apply {
            inputType = EditorInfo.TYPE_CLASS_NUMBER or EditorInfo.TYPE_NUMBER_VARIATION_PASSWORD
        }
        assertTrue("应识别为数字密码框", InputTypeUtils.isPasswordType(numInfo))

        val normalInfo = EditorInfo().apply {
            inputType = EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }
        assertFalse("普通邮箱框不应识别为密码框", InputTypeUtils.isPasswordType(normalInfo))
    }

    @Test
    fun testIsNoSuggestions() {
        val info = EditorInfo().apply {
            inputType = EditorInfo.TYPE_CLASS_TEXT or EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        }
        assertTrue("应识别为明确禁用建议", InputTypeUtils.isNoSuggestions(info))

        val normalInfo = EditorInfo().apply {
            inputType = EditorInfo.TYPE_CLASS_TEXT
        }
        assertFalse("默认不应识别为禁用建议", InputTypeUtils.isNoSuggestions(normalInfo))
    }

    @Test
    fun testResolveTypeKey() {
        val numInfo = EditorInfo().apply { inputType = EditorInfo.TYPE_CLASS_NUMBER }
        assertEquals("number", InputTypeUtils.resolveTypeKey(numInfo))

        val phoneInfo = EditorInfo().apply { inputType = EditorInfo.TYPE_CLASS_PHONE }
        assertEquals("phone", InputTypeUtils.resolveTypeKey(phoneInfo))

        val textInfo = EditorInfo().apply { inputType = EditorInfo.TYPE_CLASS_TEXT }
        assertEquals("text", InputTypeUtils.resolveTypeKey(textInfo))
        
        assertEquals("text", InputTypeUtils.resolveTypeKey(null))
    }
}
