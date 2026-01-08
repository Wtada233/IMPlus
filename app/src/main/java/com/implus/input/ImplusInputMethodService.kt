package com.implus.input

import android.content.Context
import android.inputmethodservice.InputMethodService
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import android.widget.RelativeLayout
import com.implus.input.layout.*

class ImplusInputMethodService : InputMethodService() {

    private lateinit var keyboardView: ImplusKeyboardView
    private lateinit var candidateContainer: RelativeLayout
    private lateinit var btnClose: ImageView
    private var currentLayout: KeyboardLayout? = null
    
    // 追踪所有按键状态的 Map (Framework 核心)
    private val activeStates = mutableMapOf<String, Boolean>()

    override fun onCreateInputView(): View {
        val root = layoutInflater.inflate(R.layout.input_view, null)
        keyboardView = root.findViewById(R.id.keyboard_view)
        candidateContainer = root.findViewById(R.id.candidate_container)
        btnClose = root.findViewById(R.id.btn_close_keyboard)

        root.findViewById<View>(R.id.root_container).setOnClickListener {
            if (getSharedPreferences("implus_prefs", Context.MODE_PRIVATE).getBoolean("close_outside", false)) requestHideSelf(0)
        }
        root.findViewById<View>(R.id.keyboard_view).parent.let { (it as View).setOnClickListener { } }

        loadLayout("pc_layout.json")
        applyKeyboardHeight()

        keyboardView.onKeyListener = { key -> handleKey(key) }
        btnClose.setOnClickListener { requestHideSelf(0) }
        
        currentLayout?.pages?.find { it.id == "main" }?.let { keyboardView.setPage(it) }
        
        return root
    }

    private fun loadLayout(name: String) {
        currentLayout = LayoutManager.loadLayout(this, name)
        currentLayout?.let { 
            candidateContainer.visibility = if (it.showCandidates) View.VISIBLE else View.GONE
            // 初始化状态 Map
            activeStates.clear()
            it.pages.flatMap { p -> p.rows.flatMap { r -> r.keys } }.forEach { k ->
                if (k.id != null) activeStates[k.id] = false
            }
            keyboardView.activeStates = activeStates
        }
    }

    private fun applyKeyboardHeight() {
        if (!::keyboardView.isInitialized) return
        val prefs = getSharedPreferences("implus_prefs", Context.MODE_PRIVATE)
        keyboardView.layoutParams.height = (resources.displayMetrics.heightPixels * (prefs.getInt("height_percent", 35) / 100f)).toInt()
        candidateContainer.layoutParams.height = (prefs.getInt("candidate_height", 48) * resources.displayMetrics.density).toInt()
        keyboardView.requestLayout(); candidateContainer.requestLayout()
    }

    private fun handleKey(key: KeyboardKey) {
        val ic = currentInputConnection ?: return
        
        // 1. 处理粘滞逻辑 (Sticky Logic)
        if (key.sticky != null && key.id != null) {
            val cur = activeStates[key.id] ?: false
            activeStates[key.id] = !cur
            keyboardView.activeStates = activeStates // 通知 View 刷新
            return
        }

        // 2. 获取有效属性 (应用 Overrides)
        var effCode = key.code
        var effKeyEvent = key.keyEvent
        
        key.overrides?.forEach { (stateId, override) ->
            if (activeStates[stateId] == true) {
                override.code?.let { effCode = it }
                override.keyEvent?.let { effKeyEvent = it }
            }
        }

        val finalKeyEvent = effKeyEvent

        // 3. 计算 MetaState (Framework 核心: 动态映射)
        var metaState = 0
        currentLayout?.pages?.flatMap { it.rows.flatMap { r -> r.keys } }?.forEach { k ->
            if (k.id != null && activeStates[k.id] == true && k.modifierType != null) {
                metaState = metaState or when(k.modifierType) {
                    "shift" -> KeyEvent.META_SHIFT_ON
                    "ctrl" -> KeyEvent.META_CTRL_ON
                    "alt" -> KeyEvent.META_ALT_ON
                    "meta" -> KeyEvent.META_META_ON
                    else -> 0
                }
            }
        }

        // 4. 发送按键
        if (key.action != null) {
            if (key.action.startsWith("switch_page:")) {
                currentLayout?.pages?.find { it.id == key.action.substringAfter("switch_page:") }?.let { keyboardView.setPage(it) }
            } else if (key.action == "hide") requestHideSelf(0)
        } else if (finalKeyEvent != null) {
            sendDownUpKeyEvents(finalKeyEvent)
        } else {
            when (effCode) {
                67 -> ic.deleteSurroundingText(1, 0)
                66 -> { ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, 66)); ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, 66)) }
                62 -> ic.commitText(" ", 1)
                else -> {
                    ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_DOWN, effCode, 0, metaState))
                    ic.sendKeyEvent(KeyEvent(0, 0, KeyEvent.ACTION_UP, effCode, 0, metaState))
                }
            }
        }

        // 5. 自动重置 transient 状态
        var changed = false
        currentLayout?.pages?.flatMap { it.rows.flatMap { r -> r.keys } }?.forEach { k ->
            if (k.id != null && k.sticky == "transient" && activeStates[k.id] == true) {
                activeStates[k.id] = false
                changed = true
            }
        }
        if (changed) keyboardView.activeStates = activeStates
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) { super.onStartInputView(info, restarting); applyKeyboardHeight() }
}
