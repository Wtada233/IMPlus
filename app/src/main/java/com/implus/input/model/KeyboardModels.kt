package com.implus.input.model

import com.google.gson.annotations.SerializedName

data class KeyboardLayout(
    val name: String,
    val rows: List<KeyboardRow>
)

data class KeyboardRow(
    val keys: List<KeyDefinition>,
    val rowHeight: Float = 1.0f // 相对高度权重
)

data class KeyDefinition(
    val label: String? = null,
    val code: Int? = null, // 主要键值
    val text: String? = null, // 输入的文本内容
    val weight: Float = 1.0f, // 相对宽度权重
    val popupCharacters: String? = null, // 长按弹出字符
    val icon: String? = null,
    val functional: Boolean = false // 是否为功能键（颜色可能不同）
)
