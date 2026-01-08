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

        // 处理点击外部关闭 (点击 root 但不点击 keyboardView)
        root.setOnClickListener {
            val prefs = getSharedPreferences("implus_prefs", Context.MODE_PRIVATE)
            if (prefs.getBoolean("close_outside", false)) {
                android.util.Log.d("Implus", "Outside clicked, hiding")
                requestHideSelf(0)
            }
        }
        // 阻止点击键盘内部区域触发 root 的点击事件
        keyboardView.setOnClickListener { /* 只是为了阻止冒泡 */ }

        // 1. 加载默认布局 (PC Layout)
        loadLayout("pc_layout.json")

        // 2. 应用用户自定义高度
        applyKeyboardHeight()

        // 3. 设置回调
        keyboardView.onKeyListener = { key ->
            handleKey(key)
        }
        
        // 确保初次加载时 View 已经有了页面
        currentLayout?.pages?.find { it.id == "main" }?.let {
            keyboardView.setPage(it)
        }

        btnClose.setOnClickListener {
            requestHideSelf(0)
        }
        
        return root
    }

    private fun loadLayout(fileName: String) {
        currentLayout = LayoutManager.loadLayout(this, fileName)
        currentLayout?.let { layout ->
            val mainPage = layout.pages.find { it.id == "main" }
            if (mainPage != null) {
                keyboardView.setPage(mainPage)
            }
            candidateContainer.visibility = if (layout.showCandidates) View.VISIBLE else View.GONE
        }
    }

    private fun applyKeyboardHeight() {
        if (!::keyboardView.isInitialized) return
        
        val prefs = getSharedPreferences("implus_prefs", Context.MODE_PRIVATE)
        // 默认 35%
        val percent = prefs.getInt("height_percent", 35) / 100f
        
        val displayMetrics = resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels
        val desiredHeight = (screenHeight * percent).toInt()

        val params = keyboardView.layoutParams
        if (params.height != desiredHeight) {
            params.height = desiredHeight
            keyboardView.layoutParams = params
            keyboardView.requestLayout()
        }
    }

    private fun handleKey(key: KeyboardKey) {
        val ic = currentInputConnection ?: return
        val action = key.action

        // 优先处理 Action (页面切换)
        if (action != null) {
            if (action.startsWith("switch_page:")) {
                val pageId = action.substringAfter("switch_page:")
                val page = currentLayout?.pages?.find { it.id == pageId }
                if (page != null) {
                    keyboardView.setPage(page)
                }
                return
            } else if (action == "hide") {
                requestHideSelf(0)
                return
            }
        }
        
        // Sticky Logic
        if (key.sticky != null) {
             if (key.sticky == "transient") { // Shift
                 isShifted = !isShifted
                 keyboardView.isShifted = isShifted
             } else if (key.sticky == "permanent") { // CapsLock
                 isCapsLock = !isCapsLock
                 keyboardView.isCapsLock = isCapsLock
                 // CapsLock doesn't reset Shift? Usually independent.
             }
             return
        }

        // Processing Key Code
        var code = key.code
        
        // If Custom KeyEvent is defined, use it directly (e.g. Power button, volume, etc - though requires permission usually)
        if (key.keyEvent != null) {
            sendDownUpKeyEvents(key.keyEvent)
            // Reset sticky shift
             if (isShifted) {
                 isShifted = false
                 keyboardView.isShifted = false
             }
            return
        }

        when (code) {
            -1 -> { /* Shift handled by sticky prop now */ }
            -2 -> { /* Mode switch handled by action */ }
            67 -> { // Backspace
                ic.deleteSurroundingText(1, 0)
            }
            66 -> { // Enter
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
                ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
            }
            62 -> { // Space
                 ic.commitText(" ", 1)
            }
            else -> {
                if (code > 0) {
                    // Logic to handle symbols that change with Shift (like , -> <)
                    // If the layout defines shiftedLabel/shiftedCode, we might need manual commit
                    // But relying on Meta State is standard for KeyEvents.
                    
                    val metaState = if (isShifted || isCapsLock) KeyEvent.META_SHIFT_ON else 0
                    
                    val downEvent = KeyEvent(
                        0,
                        0,
                        KeyEvent.ACTION_DOWN,
                        code,
                        0,
                        metaState
                    )
                    val upEvent = KeyEvent(
                        0,
                        0,
                        KeyEvent.ACTION_UP,
                        code,
                        0,
                        metaState
                    )
                    
                    ic.sendKeyEvent(downEvent)
                    ic.sendKeyEvent(upEvent)
                    
                    // Reset Transient Shift
                    if (isShifted) {
                         isShifted = false
                         keyboardView.isShifted = false
                    }
                }
            }
        }
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        applyKeyboardHeight()
        
        // 动态调整全屏模式，以便点击外部
        val prefs = getSharedPreferences("implus_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("close_outside", false)) {
            window?.window?.setFlags(
                android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
            )
            // 设置一个全透明的背景点击监听 (这通常通过设置 fullscreen 或大的 View 实现)
        }
    }
}
