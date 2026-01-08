package com.implus.input.layout

import com.google.gson.annotations.SerializedName

/**
 * 键盘布局数据模型
 * 对应 JSON 根节点
 */
data class KeyboardLayout(
    @SerializedName("name") val name: String,
    @SerializedName("defaultHeight") val defaultHeight: Int = 250, // dp
    @SerializedName("showCandidates") val showCandidates: Boolean = true,
    @SerializedName("pages") val pages: List<KeyboardPage> = emptyList()
)

data class KeyboardPage(
    @SerializedName("id") val id: String = "main",
    @SerializedName("rows") val rows: List<KeyboardRow> = emptyList()
)

data class KeyboardRow(
    @SerializedName("heightRatio") val heightRatio: Float = 1.0f, // 相对于默认行高的比例
    @SerializedName("keys") val keys: List<KeyboardKey> = emptyList()
)

data class KeyboardKey(
    @SerializedName("label") val label: String? = null,
    @SerializedName("code") val code: Int = 0,               // Default KeyCode
    @SerializedName("weight") val weight: Float = 1.0f,
    @SerializedName("type") val type: KeyType = KeyType.NORMAL,
    @SerializedName("action") val action: String? = null,
    
    // New properties
    @SerializedName("sticky") val sticky: String? = null,    // "transient" (Shift) or "permanent" (CapsLock)
    @SerializedName("keyEvent") val keyEvent: Int? = null,   // Custom Android KeyEvent
    @SerializedName("shiftedLabel") val shiftedLabel: String? = null,
    @SerializedName("shiftedCode") val shiftedCode: Int? = null,
    @SerializedName("style") val style: KeyStyle = KeyStyle.NORMAL
)

enum class KeyType {
    @SerializedName("normal") NORMAL,
    @SerializedName("modifier") MODIFIER,
    @SerializedName("func") FUNC,
    @SerializedName("placeholder") PLACEHOLDER
}

enum class KeyStyle {
    @SerializedName("normal") NORMAL,      // Round, standard color
    @SerializedName("function") FUNCTION,  // Square-ish, different shade
    @SerializedName("sticky") STICKY       // Square-ish, distinct shade
}
