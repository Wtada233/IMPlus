package com.implus.input

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.KeyEvent
import com.implus.input.databinding.KeyboardBaseBinding
import com.implus.input.engine.InputEngine
import com.implus.input.engine.LatinInputEngine
import com.implus.input.model.KeyDefinition
import com.implus.input.view.CandidateAdapter
import com.implus.input.view.KeyboardView

/**
 * Implus 输入法核心服务
 */
class ImplusInputMethodService : InputMethodService(), KeyboardView.OnKeyListener {

    private var _binding: KeyboardBaseBinding? = null
    private val binding get() = _binding!!
    
    private var inputEngine: InputEngine = LatinInputEngine()
    private lateinit var candidateAdapter: CandidateAdapter

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
        
        candidateAdapter = CandidateAdapter { candidate ->
            currentInputConnection?.commitText(candidate, 1)
            inputEngine.reset()
            updateCandidates()
        }
        binding.candidateRecyclerView.adapter = candidateAdapter
        
        return binding.root
    }

    override fun onKey(key: KeyDefinition) {
        val ic = currentInputConnection ?: return
        
        // 尝试通过引擎处理
        if (inputEngine.processKey(key.code ?: 0, key.label)) {
            updateCandidates()
            return
        }

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

    private fun updateCandidates() {
        candidateAdapter.setCandidates(inputEngine.getCandidates())
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
