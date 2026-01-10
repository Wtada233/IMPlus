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
}
