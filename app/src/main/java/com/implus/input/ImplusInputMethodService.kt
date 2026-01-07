package com.implus.input

import android.inputmethodservice.InputMethodService
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import com.implus.input.databinding.KeyboardBaseBinding
import com.implus.input.engine.InputEngine
import com.implus.input.engine.LatinInputEngine
import com.implus.input.model.KeyDefinition
import com.implus.input.model.SettingsManager
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
    private lateinit var settingsManager: SettingsManager

    override fun onCreate() {
        super.onCreate()
        settingsManager = SettingsManager(this)
    }

    override fun onCreateInputView(): View {
        _binding = KeyboardBaseBinding.inflate(layoutInflater)
        
        // 设置键盘高度
        binding.keyboardContainer.layoutParams.height = settingsManager.keyboardHeight
        
        val layout = com.implus.input.model.LayoutLoader.loadLayout(this, "qwerty_en.json")
        layout?.let {
            binding.keyboardView.setLayout(it)
        }
        binding.keyboardView.setOnKeyListener(this)
        binding.keyboardView.hapticFeedbackEnabled = settingsManager.hapticEnabled
        
        candidateAdapter = CandidateAdapter { candidate ->
            currentInputConnection?.commitText(candidate, 1)
            inputEngine.reset()
            updateCandidates()
        }
        binding.candidateRecyclerView.adapter = candidateAdapter
        
        return binding.root
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        // 自动切换 Shift 状态（例如在文本输入框开头）
        if (info?.inputType?.and(EditorInfo.TYPE_MASK_CLASS) == EditorInfo.TYPE_CLASS_TEXT) {
            // 这里可以实现更复杂的自动大写逻辑
        }
    }

    private var currentLayoutName = "qwerty_en.json"

    override fun onKey(key: KeyDefinition) {
        val ic = currentInputConnection ?: return
        
        // 尝试通过引擎处理
        if (inputEngine.processKey(key.code ?: 0, key.label)) {
            updateCandidates()
            return
        }

        when (key.code) {
            -5 -> ic.deleteSurroundingText(1, 0) // Backspace
            -1 -> { // Shift
                binding.keyboardView.isShifted = !binding.keyboardView.isShifted
            }
            -2 -> { // 切换布局
                currentLayoutName = if (currentLayoutName == "qwerty_en.json") "symbols.json" else "qwerty_en.json"
                val layout = com.implus.input.model.LayoutLoader.loadLayout(this, currentLayoutName)
                layout?.let { binding.keyboardView.setLayout(it) }
            }
            10 -> ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER)) // Enter
            else -> {
                val label = key.label
                var textToCommit = key.text ?: label
                
                if (binding.keyboardView.isShifted && textToCommit?.length == 1) {
                    textToCommit = textToCommit.uppercase()
                }
                
                textToCommit?.let {
                    ic.commitText(it, 1)
                }
                
                // 输入完成后如果不是锁定状态，可以自动关闭 Shift
                // binding.keyboardView.isShifted = false 
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
