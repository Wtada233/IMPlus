package com.implus.input.layout

import com.google.gson.annotations.SerializedName

data class KeyboardLayout(
    @SerializedName("name") val name: String,
    @SerializedName("defaultHeight") val defaultHeight: Int = 250,
    @SerializedName("showCandidates") val showCandidates: Boolean = true,
    @SerializedName("pages") val pages: List<KeyboardPage> = emptyList()
)

data class KeyboardPage(
    @SerializedName("id") val id: String = "main",
    @SerializedName("rows") val rows: List<KeyboardRow> = emptyList()
)

data class KeyboardRow(
    @SerializedName("heightRatio") val heightRatio: Float = 1.0f,
    @SerializedName("keys") val keys: List<KeyboardKey> = emptyList()
)

data class KeyboardKey(
    @SerializedName("id") val id: String? = null,             // 用于追踪状态的唯一ID (如 "shift_l")
    @SerializedName("label") val label: String? = null,
    @SerializedName("code") val code: Int = 0,
    @SerializedName("weight") val weight: Float = 1.0f,
    @SerializedName("type") val type: KeyType = KeyType.NORMAL,
    @SerializedName("style") val style: KeyStyle = KeyStyle.NORMAL,
    @SerializedName("action") val action: String? = null,
    @SerializedName("sticky") val sticky: String? = null,     // "transient", "permanent"
    @SerializedName("keyEvent") val keyEvent: Int? = null,
    @SerializedName("modifierType") val modifierType: String? = null, // "shift", "ctrl", "alt", "meta"
    
    // 状态覆盖逻辑: 当 Map<String, Boolean> 中对应的 key 为 true 时，覆盖当前属性
    @SerializedName("overrides") val overrides: Map<String, KeyOverride>? = null
)

/**
 * 覆盖属性的子集
 */
data class KeyOverride(
    @SerializedName("label") val label: String? = null,
    @SerializedName("code") val code: Int? = null,
    @SerializedName("keyEvent") val keyEvent: Int? = null,
    @SerializedName("style") val style: KeyStyle? = null
)

enum class KeyType {
    @SerializedName("normal") NORMAL,
    @SerializedName("modifier") MODIFIER,
    @SerializedName("func") FUNC,
    @SerializedName("placeholder") PLACEHOLDER
}

enum class KeyStyle {
    @SerializedName("normal") NORMAL,
    @SerializedName("function") FUNCTION,
    @SerializedName("sticky") STICKY
}