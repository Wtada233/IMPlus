package com.implus.input.layout

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

data class KeyboardLayout(
    @SerializedName("name") val name: String,
    @SerializedName("defaultHeight") val defaultHeight: Int = 250,
    @SerializedName("showCandidates") val showCandidates: Boolean = true,
    @SerializedName("pages") val pages: List<KeyboardPage> = emptyList()
)

data class LanguageConfig(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("engine") val engine: String = "raw",
    @SerializedName("pcLayout") val pcLayout: String = "pc_layout.json",
    @SerializedName("mobileLayout") val mobileLayout: String = "mobile_layout.json",
    @SerializedName("defaultPage") val defaultPage: String = "main",
    @SerializedName("engineConfig") val engineConfig: Map<String, String> = emptyMap()
)

data class KeyboardPage(
    @SerializedName("id") val id: String = "main",
    @SerializedName("groupId") val groupId: String? = null,
    @SerializedName("rows") val rows: List<KeyboardRow> = emptyList()
)

data class KeyboardRow(
    @SerializedName("keys") val keys: List<KeyboardKey> = emptyList()
)

data class KeyboardKey(
    @SerializedName("id") val id: String? = null,             // 状态 ID
    @SerializedName("label") val label: String? = null,
    @SerializedName("text") val text: JsonElement? = null,    // 核心输入：支持 String 或 Number (KeyCode)
    @SerializedName("weight") val weight: Float = 1.0f,
    @SerializedName("type") val type: KeyType = KeyType.NORMAL,
    @SerializedName("style") val style: KeyStyle = KeyStyle.NORMAL,
    @SerializedName("action") val action: String? = null,
    @SerializedName("sticky") val sticky: String? = null,     // "transient", "permanent"
    @SerializedName("metaValue") val metaValue: Int? = null,  // 对应的 MetaState 位掩码
    
    @SerializedName("overrides") val overrides: Map<String, KeyOverride>? = null
)

data class KeyOverride(
    @SerializedName("label") val label: String? = null,
    @SerializedName("text") val text: JsonElement? = null,
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
