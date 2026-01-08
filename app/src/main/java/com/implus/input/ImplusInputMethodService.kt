package com.implus.input

import android.content.Context
import android.inputmethodservice.InputMethodService
import android.os.SystemClock
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
    
    // 框架核心状态：仅追踪 JSON 中定义的 ID
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
        
        return root
    }

    private fun loadLayout(name: String) {
        currentLayout = LayoutManager.loadLayout(this, name)
        currentLayout?.let { layout ->
            candidateContainer.visibility = if (layout.showCandidates) View.VISIBLE else View.GONE
            activeStates.clear()
            // 预初始化所有具有 ID 的按键状态为 false
            layout.pages.flatMap { it.rows }.flatMap { it.keys }.forEach { k ->
                k.id?.let { activeStates[it] = false }
            }
            keyboardView.activeStates = activeStates
            layout.pages.find { it.id == "main" }?.let { keyboardView.setPage(it) }
        }
    }

    private fun handleKey(key: KeyboardKey) {
        val ic = currentInputConnection ?: return
        
        // 1. 粘滞逻辑：完全由 JSON 的 id 和 sticky 属性驱动
        if (key.sticky != null && key.id != null) {
            activeStates[key.id] = !(activeStates[key.id] ?: false)
            keyboardView.activeStates = activeStates
            return
        }

        // 2. 属性覆盖逻辑：检查当前哪些活跃状态被当前按键“监听” (on/overrides)
        var effCode = key.code
        var effKeyEvent = key.keyEvent
        key.overrides?.forEach { (stateId, override) ->
            if (activeStates[stateId] == true) {
                override.code?.let { effCode = it }
                override.keyEvent?.let { effKeyEvent = it }
            }
        }

        // 3. 动态 MetaState 计算：汇总所有活跃按键的 metaValue
        var totalMeta = 0
        currentLayout?.pages?.flatMap { it.rows }?.flatMap { it.keys }?.forEach { k ->
            if (k.id != null && activeStates[k.id] == true) {
                k.metaValue?.let { totalMeta = totalMeta or it }
            }
        }

        // 4. 执行输出
        val now = SystemClock.uptimeMillis()
        if (key.action != null) {
            handleAction(key.action)
        } else {
            val finalEvent = effKeyEvent
            if (finalEvent != null) {
                ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, finalEvent, 0, totalMeta))
                ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, finalEvent, 0, totalMeta))
            } else {
                when (effCode) {
                    67 -> ic.deleteSurroundingText(1, 0)
                    62 -> ic.commitText(" ", 1)
                    else -> {
                        ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, effCode, 0, totalMeta))
                        ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, effCode, 0, totalMeta))
                    }
                }
            }
        }

        // 5. 自动消耗 transient 状态：所有标记为 transient 且处于激活状态的 ID 重置
        var stateChanged = false
        currentLayout?.pages?.flatMap { it.rows }?.flatMap { it.keys }?.forEach { k ->
            if (k.id != null && k.sticky == "transient" && activeStates[k.id] == true) {
                activeStates[k.id] = false
                stateChanged = true
            }
        }
        if (stateChanged) keyboardView.activeStates = activeStates
    }

    private fun handleAction(action: String) {
        if (action.startsWith("switch_page:")) {
            val pageId = action.substringAfter("switch_page:")
            currentLayout?.pages?.find { it.id == pageId }?.let { keyboardView.setPage(it) }
        } else if (action == "hide") requestHideSelf(0)
    }

    private fun applyKeyboardHeight() {
        if (!::keyboardView.isInitialized) return
        val prefs = getSharedPreferences("implus_prefs", Context.MODE_PRIVATE)
        keyboardView.layoutParams.height = (resources.displayMetrics.heightPixels * (prefs.getInt("height_percent", 35) / 100f)).toInt()
        candidateContainer.layoutParams.height = (prefs.getInt("candidate_height", 48) * resources.displayMetrics.density).toInt()
        keyboardView.requestLayout(); candidateContainer.requestLayout()
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) { super.onStartInputView(info, restarting); applyKeyboardHeight() }
}