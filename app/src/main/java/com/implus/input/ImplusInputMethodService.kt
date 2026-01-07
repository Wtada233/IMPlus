package com.implus.input

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import com.implus.input.databinding.KeyboardBaseBinding

/**
 * Implus 输入法核心服务
 */
class ImplusInputMethodService : InputMethodService() {

    private var _binding: KeyboardBaseBinding? = null
    private val binding get() = _binding!!

    override fun onCreate() {
        super.onCreate()
        // TODO: 初始化 RIME 引擎
    }

    override fun onCreateInputView(): View {
        _binding = KeyboardBaseBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        // 根据输入框类型调整布局
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
