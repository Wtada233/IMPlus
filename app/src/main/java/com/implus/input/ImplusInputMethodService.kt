package com.implus.input

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo

/**
 * Implus 输入法主服务
 * 负责管理输入法的生命周期和视图创建
 */
class ImplusInputMethodService : InputMethodService() {

    override fun onCreate() {
        super.onCreate()
        // 初始化逻辑
    }

    override fun onCreateInputView(): View {
        // 返回我们的自定义键盘视图
        return ImplusKeyboardView(this)
    }

    override fun onStartInput(attribute: EditorInfo?, restarting: Boolean) {
        super.onStartInput(attribute, restarting)
        // 输入开始时的逻辑处理
    }

    override fun onFinishInput() {
        super.onFinishInput()
        // 输入结束时的清理
    }
}
