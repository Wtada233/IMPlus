package com.implus.input.utils

import android.view.inputmethod.EditorInfo

object InputTypeUtils {
    /**
     * 判断是否为密码输入类型
     */
    fun isPasswordType(info: EditorInfo?): Boolean {
        if (info == null) return false
        val variation = info.inputType and EditorInfo.TYPE_MASK_VARIATION
        return variation == EditorInfo.TYPE_TEXT_VARIATION_PASSWORD ||
               variation == EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
               variation == EditorInfo.TYPE_TEXT_VARIATION_WEB_PASSWORD ||
               variation == EditorInfo.TYPE_NUMBER_VARIATION_PASSWORD
    }

    /**
     * 判断是否明确禁用了联想词提示
     */
    fun isNoSuggestions(info: EditorInfo?): Boolean {
        if (info == null) return false
        return (info.inputType and EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS) != 0
    }
    
    /**
     * 判断是否为数字/电话等特殊页面
     */
    fun resolveTypeKey(info: EditorInfo?): String {
        if (info == null) return "text"
        val inputClass = info.inputType and EditorInfo.TYPE_MASK_CLASS
        return when (inputClass) {
            EditorInfo.TYPE_CLASS_NUMBER -> "number"
            EditorInfo.TYPE_CLASS_PHONE -> "phone"
            EditorInfo.TYPE_CLASS_DATETIME -> "datetime"
            else -> "text"
        }
    }
}
