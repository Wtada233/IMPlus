import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import com.implus.input.databinding.KeyboardBaseBinding
import com.implus.input.engine.InputEngine
import com.implus.input.engine.LatinInputEngine
import com.implus.input.engine.RimeInputEngine
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
    
    private lateinit var latinEngine: InputEngine
    private lateinit var rimeEngine: InputEngine
    private var currentEngine: InputEngine? = null

    private lateinit var candidateAdapter: CandidateAdapter
    private lateinit var settingsManager: SettingsManager

    override fun onCreate() {
        super.onCreate()
        settingsManager = SettingsManager(this)
        latinEngine = LatinInputEngine()
        rimeEngine = RimeInputEngine(this)
        currentEngine = rimeEngine // 默认开启中文模式
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
            currentEngine?.reset()
            updateCandidates()
        }
        binding.candidateRecyclerView.adapter = candidateAdapter
        
        return binding.root
    }

    override fun onKey(key: KeyDefinition) {
        val ic = currentInputConnection ?: return
        
        // 尝试通过引擎处理
        if (currentEngine?.processKey(key.code ?: 0, key.label) == true) {
            // 检查引擎是否有直接上屏的文本 (Commit)
            if (currentEngine is RimeInputEngine) {
                val commitText = com.osfans.trime.Rime.getCommit()
                if (commitText != null) {
                    ic.commitText(commitText, 1)
                }
            }
            updateCandidates()
            return
        }

        when (key.code) {
            -5 -> {
                if (currentEngine?.getCandidates()?.isNotEmpty() == true) {
                    currentEngine?.reset()
                    updateCandidates()
                } else {
                    ic.deleteSurroundingText(1, 0)
                }
            }
            -1 -> { // Shift
                binding.keyboardView.isShifted = !binding.keyboardView.isShifted
            }
            -2 -> { // 切换布局
                currentLayoutName = if (currentLayoutName == "qwerty_en.json") "symbols.json" else "qwerty_en.json"
                val layout = com.implus.input.model.LayoutLoader.loadLayout(this, currentLayoutName)
                layout?.let { binding.keyboardView.setLayout(it) }
            }
            32 -> { // Space
                val candidates = currentEngine?.getCandidates()
                if (candidates != null && candidates.isNotEmpty()) {
                    ic.commitText(candidates[0], 1)
                    currentEngine?.reset()
                    updateCandidates()
                } else {
                    ic.commitText(" ", 1)
                }
            }
            10 -> ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
            else -> {
                val label = key.label
                var textToCommit = key.text ?: label
                
                if (binding.keyboardView.isShifted && textToCommit?.length == 1) {
                    textToCommit = textToCommit.uppercase()
                }
                
                textToCommit?.let {
                    ic.commitText(it, 1)
                }
            }
        }
    }

    private fun updateCandidates() {
        candidateAdapter.setCandidates(currentEngine?.getCandidates() ?: emptyList())
    }
