package com.implus.input.utils

object Constants {
    // 延时常量
    const val PRESS_DELAY_MS = 10L
    const val RELEASE_DELAY_MS = 20L
    const val LOAD_LAYOUT_DELAY_MS = 50L // 用于 postDelayed 等

    // UI 数值
    const val DEFAULT_CURSOR_OFFSET = 1
    const val ALPHA_OPAQUE = 255
    const val ALPHA_SEMI_TRANSPARENT = 100
    const val MAX_PERCENT = 100
    const val HEX_BASE = 16
    const val DEFAULT_RIPPLE_COLOR = 0x40FFFFFF

    // 默认高度与阈值
    const val MIN_CANDIDATE_HEIGHT_DP = 30
    const val MIN_HEIGHT_PERCENT = 20
    const val MIN_SWIPE_THRESHOLD = 10
    const val MIN_ANIM_DURATION_MS = 10

    // 按键重复延时
    const val REPEAT_DELAY_MS = 50L
    const val INITIAL_REPEAT_DELAY_MS = 400L

    // 日志标签
    const val TAG_SERVICE = "ImplusIME"
    const val TAG_VIEW = "ImplusView"

    // Panel Defaults
    const val PANEL_EMPTY_VIEW_PADDING = 32
    const val PANEL_ITEM_PADDING_HORIZONTAL = 16
    const val PANEL_ITEM_PADDING_VERTICAL = 12
    const val PANEL_ITEM_TEXT_SIZE = 16f
    const val PANEL_DIVIDER_HEIGHT = 1

    // Keyboard View Defaults
    const val KEY_TEXT_SIZE_RATIO = 0.4f
    const val KEY_MAX_TEXT_WIDTH_RATIO = 0.8f

    // Default Colors (Fallback)
    const val COLOR_DEFAULT_BG = -16777216 // Color.BLACK
    const val COLOR_DEFAULT_KEY_BG = -12303292 // Color.DKGRAY
    const val COLOR_DEFAULT_FUNC_KEY_BG = -7829368 // Color.GRAY
    const val COLOR_DEFAULT_KEY_TEXT = -1 // Color.WHITE
    const val COLOR_DEFAULT_FUNC_KEY_TEXT = -1 // Color.WHITE
    const val COLOR_DEFAULT_STICKY_INACTIVE = -7829368 // Color.GRAY
    const val COLOR_DEFAULT_STICKY_ACTIVE = -16776961 // Color.BLUE
    const val COLOR_DEFAULT_STICKY_TEXT_ACTIVE = -1 // Color.WHITE
    
    const val COLOR_PANEL_BG_DEFAULT = -12303292 // Color.DKGRAY
    const val COLOR_TOOLBAR_BG_DEFAULT = -16777216 // Color.BLACK
    const val COLOR_ACCENT_ERROR_DEFAULT = -65536 // Color.RED
    const val COLOR_DIVIDER_DEFAULT = -3355444 // Color.LTGRAY
}
