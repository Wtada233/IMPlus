package com.implus.input.layout

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

data class KeyboardLayout(
    @SerializedName("name") val name: String,
    @SerializedName("defaultHeight") val defaultHeight: Int = 250,
    @SerializedName("showCandidates") val showCandidates: Boolean = true,
    @SerializedName("useDictionary") val useDictionary: Boolean = false,
    @SerializedName("theme") val theme: KeyboardTheme? = null, // 旧版兼容
    @SerializedName("theme_light") val themeLight: KeyboardTheme? = null,
    @SerializedName("theme_dark") val themeDark: KeyboardTheme? = null,
    @SerializedName("pages") val pages: List<KeyboardPage> = emptyList()
)

data class KeyboardTheme(
    @SerializedName("background") val background: String? = null,
    @SerializedName("keyBackground") val keyBackground: String? = null,
    @SerializedName("keyText") val keyText: String? = null,
    @SerializedName("functionKeyBackground") val functionKeyBackground: String? = null,
    @SerializedName("functionKeyText") val functionKeyText: String? = null,
    @SerializedName("stickyInactiveBackground") val stickyInactiveBackground: String? = null,
    @SerializedName("stickyActiveBackground") val stickyActiveBackground: String? = null,
    @SerializedName("stickyActiveText") val stickyActiveText: String? = null,
    @SerializedName("rippleColor") val rippleColor: String? = null
)

data class LanguageConfig(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("engine") val engine: String = "raw",
    @SerializedName("dictionary") val dictionary: String? = null, // 词典文件名
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
    
    @SerializedName("overrides") val overrides: Map<String, KeyOverride>? = null,

    @Transient var parsedKeyCode: Int = android.view.KeyEvent.KEYCODE_UNKNOWN
)

data class KeyOverride(
    @SerializedName("label") val label: String? = null,
    @SerializedName("text") val text: JsonElement? = null,
    @SerializedName("style") val style: KeyStyle? = null,

    @Transient var parsedKeyCode: Int = android.view.KeyEvent.KEYCODE_UNKNOWN
)

enum class KeyType {
    @SerializedName("normal") NORMAL,
    @SerializedName("modifier") MODIFIER,
    @SerializedName("func") FUNC
}

enum class KeyStyle {
    @SerializedName("normal") NORMAL,
    @SerializedName("function") FUNCTION,
    @SerializedName("sticky") STICKY
}
