package com.implus.input

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.KeyEvent
import com.implus.input.databinding.KeyboardBaseBinding
import com.implus.input.model.KeyDefinition
import com.implus.input.view.KeyboardView

/**
 * Implus 输入法核心服务
 */
class ImplusInputMethodService : InputMethodService(), KeyboardView.OnKeyListener {

    private var _binding: KeyboardBaseBinding? = null
    private val binding get() = _binding!!

    override fun onCreate() {
        super.onCreate()
        // TODO: 初始化 RIME 引擎
    }

    override fun onCreateInputView(): View {
        _binding = KeyboardBaseBinding.inflate(layoutInflater)
        
        val layout = com.implus.input.model.LayoutLoader.loadLayout(this, "qwerty_en.json")
        layout?.let {
            binding.keyboardView.setLayout(it)
        }
        binding.keyboardView.setOnKeyListener(this)
        
        return binding.root
    }

    override fun onKey(key: KeyDefinition) {
        val ic = currentInputConnection ?: return
        
        when (key.code) {
            -5 -> ic.deleteSurroundingText(1, 0) // Backspace
            -1 -> { /* TODO: Shift Logic */ }
            10 -> ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER)) // Enter
            else -> {
                val textToCommit = key.text ?: key.label
                textToCommit?.let {
                    ic.commitText(it, 1)
                }
            }
        }
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
