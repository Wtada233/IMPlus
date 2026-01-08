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
    @SerializedName("keys") val keys: List<KeyboardKey> = emptyList()
)

data class KeyboardKey(
    @SerializedName("id") val id: String? = null,             // 状态 ID
    @SerializedName("label") val label: String? = null,
    @SerializedName("code") val code: Int = 0,
    @SerializedName("weight") val weight: Float = 1.0f,
    @SerializedName("type") val type: KeyType = KeyType.NORMAL,
    @SerializedName("style") val style: KeyStyle = KeyStyle.NORMAL,
    @SerializedName("action") val action: String? = null,
    @SerializedName("sticky") val sticky: String? = null,     // "transient", "permanent"
    @SerializedName("keyEvent") val keyEvent: Int? = null,
    @SerializedName("metaValue") val metaValue: Int? = null,  // 对应的 MetaState 位掩码 (如 1 为 Shift)
    
    // 逻辑覆盖：当指定的 ID 处于激活状态时，应用这些属性
    // 这里的 Key 是状态 ID (如 "shift")
    @SerializedName("overrides") val overrides: Map<String, KeyOverride>? = null
)

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
