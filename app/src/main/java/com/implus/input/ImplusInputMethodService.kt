package com.implus.input

import android.content.Context
import android.content.Intent
import android.content.ClipboardManager
import android.inputmethodservice.InputMethodService
import android.os.SystemClock
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.util.Log
import com.implus.input.layout.*
import com.implus.input.engine.*
import com.implus.input.manager.ClipboardHistoryManager
import com.implus.input.manager.PanelManager

import android.content.SharedPreferences

class ImplusInputMethodService : InputMethodService(), ClipboardManager.OnPrimaryClipChangedListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private lateinit var keyboardView: ImplusKeyboardView
    private lateinit var candidateContainer: RelativeLayout
    private lateinit var btnClose: ImageView
    private var currentLayout: KeyboardLayout? = null
    private var currentLanguage: LanguageConfig? = null
    private var inputEngine: InputEngine = RawEngine()
    
    // 框架核心状态：仅追踪 JSON 中定义的 ID
    private val activeStates = mutableMapOf<String, Boolean>()
    // 追踪当前处于激活状态的 transient (瞬时) 按键 ID，优化重置性能
    private val activeTransientKeyIds = mutableSetOf<String>()
    // 缓存 StateID -> MetaValue 的映射，避免每次按键遍历全布局
    private val metaStateMap = mutableMapOf<String, Int>()

    private lateinit var toolbarView: View
    private lateinit var candidateScroll: View
    private lateinit var pageIndicator: android.widget.LinearLayout
    private lateinit var panelManager: PanelManager

    private val prefs by lazy { getSharedPreferences("implus_prefs", Context.MODE_PRIVATE) }

    override fun onCreate() {
        super.onCreate()
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.addPrimaryClipChangedListener(this)
        prefs.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.removePrimaryClipChangedListener(this)
        prefs.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == "current_lang" || key?.startsWith("use_pc_layout_") == true) {
            reloadLanguage()
        } else if (key == "height_percent" || key == "candidate_height" || key == "swipe_threshold" 
                   || key == "horizontal_spacing" || key == "vertical_spacing"
                   || key == "vibration_enabled" || key == "vibration_strength") {
            applyKeyboardSettings()
        }
    }

    override fun onPrimaryClipChanged() {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = cm.primaryClip
        if (clip != null && clip.itemCount > 0) {
            val text = clip.getItemAt(0).text
            if (!text.isNullOrBlank()) {
                ClipboardHistoryManager.add(this, text.toString())
            }
        }
    }

    override fun onCreateInputView(): View {
        val themedContext = android.view.ContextThemeWrapper(this, R.style.Theme_Implus)
        val root = android.view.LayoutInflater.from(themedContext).inflate(R.layout.input_view, null)
        keyboardView = root.findViewById(R.id.keyboard_view)
        candidateContainer = root.findViewById(R.id.candidate_container)
        btnClose = root.findViewById(R.id.btn_close_keyboard)
        toolbarView = root.findViewById(R.id.toolbar_view)
        candidateScroll = root.findViewById(R.id.candidate_scroll)
        pageIndicator = root.findViewById(R.id.page_indicator)

        panelManager = PanelManager(root, { text ->
            currentInputConnection?.commitText(text, 1)
        }, {
            // Back to keyboard callback if needed
        })

        setupToolbar(root)
        setupEditPad(root)
        
        root.findViewById<View>(R.id.root_container).setOnClickListener {
            if (getSharedPreferences("implus_prefs", Context.MODE_PRIVATE).getBoolean("close_outside", false)) requestHideSelf(0)
        }
        root.findViewById<View>(R.id.keyboard_view).parent.let { (it as View).setOnClickListener { } }

        reloadLanguage()
        applyKeyboardSettings()

        keyboardView.onKeyListener = { key -> handleKey(key) }
        
        // Gesture Logic
        keyboardView.onSwipeListener = { direction ->
            val allPages = currentLayout?.pages ?: emptyList()
            val currentId = keyboardView.getCurrentPageId()
            val currentPage = allPages.find { it.id == currentId }
            
            if (currentPage != null) {
                val groupPages = if (currentPage.groupId != null) {
                    allPages.filter { it.groupId == currentPage.groupId }
                } else {
                    listOf(currentPage)
                }

                if (groupPages.size > 1) {
                    val currentIndex = groupPages.indexOf(currentPage)
                    val nextIndex = if (direction == ImplusKeyboardView.Direction.LEFT) {
                        (currentIndex + 1) % groupPages.size
                    } else {
                        if (currentIndex - 1 < 0) groupPages.size - 1 else currentIndex - 1
                    }
                    val targetPage = groupPages[nextIndex]
                    keyboardView.setPage(targetPage, direction)
                    updatePageIndicator(targetPage)
                }
            }
        }

        btnClose.setOnClickListener { requestHideSelf(0) }
        
        return root
    }

    private fun updatePageIndicator(currentPage: KeyboardPage) {
        pageIndicator.removeAllViews()
        val allPages = currentLayout?.pages ?: emptyList()
        val groupPages = if (currentPage.groupId != null) {
            allPages.filter { it.groupId == currentPage.groupId }
        } else {
            emptyList()
        }

        if (groupPages.size <= 1) {
            pageIndicator.visibility = View.GONE
            return
        }

        pageIndicator.visibility = View.VISIBLE
        val currentIndex = groupPages.indexOf(currentPage)
        
        for (i in groupPages.indices) {
            val dot = View(this)
            val size = (6 * resources.displayMetrics.density).toInt()
            val margin = (4 * resources.displayMetrics.density).toInt()
            val params = android.widget.LinearLayout.LayoutParams(size, size)
            params.setMargins(margin, 0, margin, 0)
            dot.layoutParams = params
            
            val drawable = android.graphics.drawable.GradientDrawable()
            drawable.shape = android.graphics.drawable.GradientDrawable.OVAL
            if (i == currentIndex) {
                drawable.setColor(android.graphics.Color.WHITE)
                drawable.alpha = 255
            } else {
                drawable.setColor(android.graphics.Color.GRAY)
                drawable.alpha = 100
            }
            dot.background = drawable
            pageIndicator.addView(dot)
        }
    }

    private fun setupToolbar(root: View) {
        root.findViewById<View>(R.id.btn_settings).setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
        root.findViewById<View>(R.id.btn_edit_mode).setOnClickListener {
            panelManager.switchPanel(PanelManager.PanelType.EDIT)
        }
        root.findViewById<View>(R.id.btn_clipboard).setOnClickListener {
            // Toggle clipboard
            val isCurrentlyClipboard = root.findViewById<View>(R.id.clipboard_view).visibility == View.VISIBLE
            if (isCurrentlyClipboard) {
                panelManager.switchPanel(PanelManager.PanelType.KEYBOARD)
            } else {
                panelManager.switchPanel(PanelManager.PanelType.CLIPBOARD)
            }
        }
    }

    private fun setupEditPad(root: View) {
        root.findViewById<View>(R.id.btn_select_all).setOnClickListener {
            currentInputConnection?.performContextMenuAction(android.R.id.selectAll)
        }
        root.findViewById<View>(R.id.btn_copy).setOnClickListener {
            currentInputConnection?.performContextMenuAction(android.R.id.copy)
        }
        root.findViewById<View>(R.id.btn_pad_paste).setOnClickListener {
            currentInputConnection?.performContextMenuAction(android.R.id.paste)
        }
        
        setupRepeatKey(root.findViewById(R.id.btn_arrow_left), KeyEvent.KEYCODE_DPAD_LEFT)
        setupRepeatKey(root.findViewById(R.id.btn_arrow_right), KeyEvent.KEYCODE_DPAD_RIGHT)
        setupRepeatKey(root.findViewById(R.id.btn_arrow_up), KeyEvent.KEYCODE_DPAD_UP)
        setupRepeatKey(root.findViewById(R.id.btn_arrow_down), KeyEvent.KEYCODE_DPAD_DOWN)
        
        root.findViewById<View>(R.id.btn_back_to_keyboard).setOnClickListener {
            panelManager.switchPanel(PanelManager.PanelType.KEYBOARD)
        }
    }

    private fun setupRepeatKey(view: View, keyCode: Int) {
        view.setOnTouchListener(object : View.OnTouchListener {
            private var handler: android.os.Handler? = null
            private val runnable = object : Runnable {
                override fun run() {
                    sendKey(keyCode, 0)
                    handler?.postDelayed(this, 50)
                }
            }

            override fun onTouch(v: View, event: android.view.MotionEvent): Boolean {
                when (event.action) {
                    android.view.MotionEvent.ACTION_DOWN -> {
                        if (handler == null) handler = android.os.Handler(android.os.Looper.getMainLooper())
                        sendKey(keyCode, 0)
                        handler?.postDelayed(runnable, 400) // Initial delay
                        v.isPressed = true
                        return true
                    }
                    android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                        handler?.removeCallbacks(runnable)
                        v.isPressed = false
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun reloadLanguage() {
        val prefs = getSharedPreferences("implus_prefs", Context.MODE_PRIVATE)
        val langId = prefs.getString("current_lang", "en") ?: "en"
        val isPcLayout = prefs.getBoolean("use_pc_layout_$langId", true)

        val config = LayoutManager.loadLanguageConfig(this, langId)
        if (config != null) {
            currentLanguage = config
            val layoutFile = if (isPcLayout) config.pcLayout else config.mobileLayout
            loadLayout(langId, layoutFile)
            setupEngine(config.engine)
        }
        updateCandidates()
    }

    private fun setupEngine(engineType: String) {
        inputEngine = when (engineType) {
            "raw" -> RawEngine()
            else -> RawEngine()
        }
    }

    private fun loadLayout(langId: String, fileName: String) {
        val startTime = SystemClock.elapsedRealtime()
        currentLayout = LayoutManager.loadLayout(this, langId, fileName)
        currentLayout?.let { layout ->
            Log.d("Implus", "Layout loaded in ${SystemClock.elapsedRealtime() - startTime}ms")
            keyboardView.theme = layout.theme
            // Respect layout config for candidate container visibility
            candidateContainer.visibility = if (layout.showCandidates) View.VISIBLE else View.GONE
            
            activeStates.clear()
            activeTransientKeyIds.clear()
            metaStateMap.clear()
            
            layout.pages.flatMap { it.rows }.flatMap { it.keys }.forEach { k ->
                k.id?.let { 
                    activeStates[it] = false
                    // Cache meta value for this state ID
                    k.metaValue?.let { meta -> metaStateMap[it] = meta }
                }
                
                // Pre-parse KeyCodes for performance
                k.parsedKeyCode = parseKeyCode(k.text)
                k.overrides?.values?.forEach { override ->
                    override.parsedKeyCode = parseKeyCode(override.text)
                }
            }
            keyboardView.activeStates = activeStates
            val mainPage = layout.pages.find { it.id == "main" }
            if (mainPage != null) {
                keyboardView.setPage(mainPage)
                updatePageIndicator(mainPage)
            }
        }
    }

    private fun parseKeyCode(text: com.google.gson.JsonElement?): Int {
        if (text == null || !text.isJsonPrimitive) return KeyEvent.KEYCODE_UNKNOWN
        val p = text.asJsonPrimitive
        return if (p.isNumber) {
            p.asInt
        } else if (p.isString) {
            val str = p.asString
            val code = KeyEvent.keyCodeFromString("KEYCODE_${str.uppercase()}")
            if (code != KeyEvent.KEYCODE_UNKNOWN) code else KeyEvent.KEYCODE_UNKNOWN
        } else {
            KeyEvent.KEYCODE_UNKNOWN
        }
    }

    private fun handleKey(key: KeyboardKey) {
        val ic = currentInputConnection ?: return

        // 1. 引擎优先：如果输入引擎（如拼音）截获了此键，则直接返回
        if (inputEngine.processKey(key, ic, activeStates)) {
            updateCandidates()
            return
        }
        
        // 2. 状态切换逻辑 (完全数据驱动，基于 JSON 的 sticky 属性)
        if (key.sticky != null && key.id != null) {
            val currentState = activeStates[key.id] ?: false
            activeStates[key.id] = !currentState
            if (key.sticky == "transient") {
                if (!currentState) activeTransientKeyIds.add(key.id) else activeTransientKeyIds.remove(key.id)
            }
            keyboardView.activeStates = activeStates
            return
        }

        // 3. 确定最终生效的输入内容 (考虑 overrides)
        var effInput = key.text
        var effKeyCode = key.parsedKeyCode
        key.overrides?.forEach { (stateId, override) ->
            if (activeStates[stateId] == true) {
                override.text?.let { 
                    effInput = it 
                    effKeyCode = override.parsedKeyCode
                }
            }
        }

        // 4. 计算组合键状态 (Meta State)
        val totalMeta = calculateTotalMetaState()

        // 5. 执行动作或发送输入
        if (key.action != null) {
            handleAction(key.action)
        } else {
            dispatchInput(effKeyCode, effInput, totalMeta, ic)
        }

        // 6. 行为后置处理：重置所有“瞬时”状态（如按完 Shift 后恢复）
        resetTransientStates()
        updateCandidates()
    }

    private fun calculateTotalMetaState(): Int {
        var meta = 0
        activeStates.forEach { (stateId, isActive) ->
            if (isActive) {
                metaStateMap[stateId]?.let { value ->
                    meta = meta or value
                    // Android 兼容性：补全左右掩码
                    when (value) {
                        KeyEvent.META_CTRL_ON -> meta = meta or KeyEvent.META_CTRL_LEFT_ON
                        KeyEvent.META_ALT_ON -> meta = meta or KeyEvent.META_ALT_LEFT_ON
                        KeyEvent.META_SHIFT_ON -> meta = meta or KeyEvent.META_SHIFT_LEFT_ON
                    }
                }
            }
        }
        return meta
    }

    private fun dispatchInput(keyCode: Int, text: com.google.gson.JsonElement?, meta: Int, ic: android.view.inputmethod.InputConnection) {
        if (keyCode != KeyEvent.KEYCODE_UNKNOWN) {
            sendKey(keyCode, meta)
        } else {
            text?.let { input ->
                if (input.isJsonPrimitive && input.asJsonPrimitive.isString) {
                    ic.commitText(input.asString, 1)
                }
            }
        }
    }

    private fun resetTransientStates() {
        if (activeTransientKeyIds.isNotEmpty()) {
            activeTransientKeyIds.forEach { id -> activeStates[id] = false }
            activeTransientKeyIds.clear()
            keyboardView.activeStates = activeStates
        }
    }

    private fun handleAction(action: String) {
        val ic = currentInputConnection ?: return
        when {
            action.startsWith("switch_page:") -> {
                val pageId = action.substringAfter("switch_page:")
                currentLayout?.pages?.find { it.id == pageId }?.let { 
                    keyboardView.setPage(it)
                    updatePageIndicator(it)
                }
            }
            action == "backspace" -> ic.deleteSurroundingText(1, 0) // 保持语义化的退格，比 KeyCode 兼容性更好
            action == "hide" -> requestHideSelf(0)
        }
    }

    private fun sendKey(code: Int, meta: Int) {
        val ic = currentInputConnection ?: return
        val now = SystemClock.uptimeMillis()
        ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, code, 0, meta))
        ic.sendKeyEvent(KeyEvent(now, now + 10, KeyEvent.ACTION_UP, code, 0, meta))
    }

    private fun updateCandidates() {
        val candidates = inputEngine.getCandidates()
        val candidateStrip = candidateContainer.findViewById<ViewGroup>(R.id.candidate_strip)
        candidateStrip.removeAllViews()

        if (candidates.isEmpty()) {
            toolbarView.visibility = View.VISIBLE
            candidateScroll.visibility = View.GONE
        } else {
            toolbarView.visibility = View.GONE
            candidateScroll.visibility = View.VISIBLE
            
            for (candidate in candidates) {
                val tv = TextView(this)
                tv.text = candidate
                tv.setTextColor(android.graphics.Color.WHITE)
                tv.textSize = 18f
                tv.setPadding(32, 0, 32, 0)
                tv.gravity = android.view.Gravity.CENTER
                tv.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)
                tv.setBackgroundResource(android.R.drawable.list_selector_background)
                tv.setOnClickListener {
                    currentInputConnection?.commitText(candidate, 1)
                    inputEngine.processKey(KeyboardKey(action = "commit"), currentInputConnection!!, activeStates) // Notify engine
                    updateCandidates()
                }
                candidateStrip.addView(tv)
            }
        }
    }

    private fun applyKeyboardSettings() {
        if (!::keyboardView.isInitialized) return
        val prefs = getSharedPreferences("implus_prefs", Context.MODE_PRIVATE)
        keyboardView.layoutParams.height = (resources.displayMetrics.heightPixels * (prefs.getInt("height_percent", 35) / 100f)).toInt()
        keyboardView.swipeThreshold = prefs.getInt("swipe_threshold", 50)
        keyboardView.horizontalSpacing = prefs.getInt("horizontal_spacing", 6)
        keyboardView.verticalSpacing = prefs.getInt("vertical_spacing", 6)
        keyboardView.vibrationEnabled = prefs.getBoolean("vibration_enabled", true)
        keyboardView.vibrationStrength = prefs.getInt("vibration_strength", 30)
        
        candidateContainer.layoutParams.height = (prefs.getInt("candidate_height", 48) * resources.displayMetrics.density).toInt()
        keyboardView.requestLayout(); candidateContainer.requestLayout()
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) { 
        super.onStartInputView(info, restarting)
        applyKeyboardSettings() 
    }
}
