package com.implus.input

import android.content.Context
import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import android.widget.RelativeLayout
import com.implus.input.layout.KeyboardKey
import com.implus.input.layout.KeyboardLayout
import com.implus.input.layout.LayoutManager

/**
 * Implus 输入法主服务
 */
class ImplusInputMethodService : InputMethodService() {

    private lateinit var keyboardView: ImplusKeyboardView
    private lateinit var candidateContainer: RelativeLayout
    private lateinit var btnClose: ImageView
    private var currentLayout: KeyboardLayout? = null
    
    private var isShifted = false
    private var isCapsLock = false

    override fun onCreateInputView(): View {
        val root = layoutInflater.inflate(R.layout.input_view, null)
        
        keyboardView = root.findViewById(R.id.keyboard_view)
        candidateContainer = root.findViewById(R.id.candidate_container)
        btnClose = root.findViewById(R.id.btn_close_keyboard)

        // 处理点击外部关闭
        root.findViewById<View>(R.id.root_container).setOnClickListener {
            val prefs = getSharedPreferences("implus_prefs", Context.MODE_PRIVATE)
            if (prefs.getBoolean("close_outside", false)) {
                requestHideSelf(0)
            }
        }
        
        // 阻止点击键盘区域触发 root 的点击
        root.findViewById<View>(R.id.keyboard_view).parent.let { (it as View).setOnClickListener { } }

        loadLayout("pc_layout.json")
        applyKeyboardHeight()

        keyboardView.onKeyListener = { key -> handleKey(key) }
        btnClose.setOnClickListener { requestHideSelf(0) }
        
        currentLayout?.pages?.find { it.id == "main" }?.let { keyboardView.setPage(it) }
        
        return root
    }

    private fun loadLayout(fileName: String) {
        currentLayout = LayoutManager.loadLayout(this, fileName)
        currentLayout?.let { layout ->
            candidateContainer.visibility = if (layout.showCandidates) View.VISIBLE else View.GONE
        }
    }

    private fun applyKeyboardHeight() {
        if (!::keyboardView.isInitialized) return
        
        val prefs = getSharedPreferences("implus_prefs", Context.MODE_PRIVATE)
        
        // 1. 键盘主体高度
        val percent = prefs.getInt("height_percent", 35) / 100f
        val screenHeight = resources.displayMetrics.heightPixels
        keyboardView.layoutParams.height = (screenHeight * percent).toInt()
        
        // 2. 候选栏高度
        val candHeightDp = prefs.getInt("candidate_height", 48)
        val candHeightPx = (candHeightDp * resources.displayMetrics.density).toInt()
        candidateContainer.layoutParams.height = candHeightPx
        
        keyboardView.requestLayout()
        candidateContainer.requestLayout()
    }

    private fun handleKey(key: KeyboardKey) {
        val ic = currentInputConnection ?: return
        val action = key.action

        if (action != null) {
            if (action.startsWith("switch_page:")) {
                val pageId = action.substringAfter("switch_page:")
                currentLayout?.pages?.find { it.id == pageId }?.let { keyboardView.setPage(it) }
                return
            } else if (action == "hide") {
                requestHideSelf(0)
                return
            }
        }
        
        // Sticky Logic (Decoupled)
        if (key.sticky != null) {
             if (key.sticky == "transient") { // Shift
                 isShifted = !isShifted
                 keyboardView.isShifted = isShifted
             } else if (key.sticky == "permanent") { // CapsLock
                 isCapsLock = !isCapsLock
                 keyboardView.isCapsLock = isCapsLock
             }
             return
        }

        // Processing Key
        if (key.keyEvent != null) {
            sendDownUpKeyEvents(key.keyEvent)
        } else if (key.code != 0) {
            when (key.code) {
                67 -> ic.deleteSurroundingText(1, 0)
                66 -> {
                    ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                    ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
                }
                62 -> ic.commitText(" ", 1)
                else -> {
                    val metaState = if (isShifted || isCapsLock) KeyEvent.META_SHIFT_ON else 0
                    ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, key.code, 0, metaState))
                    ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_UP, key.code, 0, metaState))
                }
            }
        }

        // Reset Transient Shift after use
        if (isShifted) {
            isShifted = false
            keyboardView.isShifted = false
        }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        applyKeyboardHeight()
    }
}