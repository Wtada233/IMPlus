package com.implus.input

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.inputmethodservice.InputMethodService
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import com.implus.input.engine.*
import com.implus.input.layout.*
import com.implus.input.manager.*

class ImplusInputMethodService : InputMethodService(), android.content.ClipboardManager.OnPrimaryClipChangedListener, SharedPreferences.OnSharedPreferenceChangeListener {

    companion object {
        private const val TAG = "ImplusInputMethodService"
        private const val ACTION_SWITCH_PAGE = "switch_page:"
    }

    private lateinit var keyboardView: ImplusKeyboardView
    private lateinit var candidateContainer: RelativeLayout
    private lateinit var btnClose: ImageView
    private var currentLayout: KeyboardLayout? = null
    private var currentLanguage: LanguageConfig? = null
    private var inputEngine: InputEngine = RawEngine()
    private val dictManager by lazy { DictionaryManager(this) }
    
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var currentLoadTaskId = 0
    
    // 框架核心状态：仅追踪 JSON 中定义的 ID
    private val activeStates = mutableMapOf<String, Boolean>()
    // 追踪当前处于激活状态的 transient (瞬时) 按键 ID，优化重置性能
    private val activeTransientKeyIds = mutableSetOf<String>()
    // 缓存 StateID -> MetaValue 的映射，避免每次按键遍历全布局
    private val metaStateMap = mutableMapOf<String, Int>()
    // 缓存 StateID -> 物理 KeyCode 的映射，用于 sendKey 时模拟物理按下
    private val metaKeyCodeMap = mutableMapOf<String, Int>()

    private lateinit var toolbarView: View
    private lateinit var candidateScroll: View
    private lateinit var candidateStrip: android.view.ViewGroup
    private lateinit var pageIndicator: android.widget.LinearLayout
    private lateinit var panelManager: PanelManager
    private var inputRootView: View? = null

    private val settings by lazy { SettingsManager(this) }
    private val assetRes by lazy { AssetResourceManager(this) }

    override fun onCreate() {
        super.onCreate()
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        cm.addPrimaryClipChangedListener(this)
        settings.registerListener(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        cm.removePrimaryClipChangedListener(this)
        settings.unregisterListener(this)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        assetRes.refresh()
        applyThemeToStaticViews()
    }

    private fun applyThemeToStaticViews() {
        val root = inputRootView ?: return
        if (!::candidateContainer.isInitialized) return
        
        val panelBg = assetRes.getColor("panel_background", android.graphics.Color.DKGRAY)
        val toolbarBg = assetRes.getColor("toolbar_background", android.graphics.Color.BLACK)
        val keyText = assetRes.getColor("key_text", android.graphics.Color.WHITE)
        val rippleColor = assetRes.getColor("ripple_color", 0x40FFFFFF)
        val accentError = assetRes.getColor("accent_error", android.graphics.Color.RED)
        val dividerColor = assetRes.getColor("divider_color", android.graphics.Color.LTGRAY)

        candidateContainer.setBackgroundColor(toolbarBg)
        toolbarView.parent?.let { (it as View).setBackgroundColor(panelBg) }
        root.findViewById<View>(R.id.clipboard_view)?.setBackgroundColor(panelBg)
        root.findViewById<View>(R.id.edit_pad_view)?.setBackgroundColor(panelBg)
        
        // 更新按钮着色 (工具栏)
        val toolbarButtons = listOf(R.id.btn_settings, R.id.btn_edit_mode, R.id.btn_clipboard, R.id.btn_close_keyboard)
        toolbarButtons.forEach { id ->
            root.findViewById<ImageView>(id)?.setColorFilter(keyText)
        }

        // 更新编辑面板 (方向键 & 功能键)
        val editButtons = listOf(R.id.btn_arrow_up, R.id.btn_arrow_down, R.id.btn_arrow_left, R.id.btn_arrow_right)
        editButtons.forEach { id ->
            root.findViewById<ImageButton>(id)?.setColorFilter(keyText)
        }

        val materialButtons = listOf(R.id.btn_select_all, R.id.btn_copy, R.id.btn_pad_paste, R.id.btn_back_to_keyboard)
        materialButtons.forEach { id ->
            root.findViewById<com.google.android.material.button.MaterialButton>(id)?.let {
                it.setTextColor(keyText)
                it.rippleColor = android.content.res.ColorStateList.valueOf(rippleColor)
                if (id == R.id.btn_back_to_keyboard) {
                    it.strokeColor = android.content.res.ColorStateList.valueOf(dividerColor)
                }
            }
        }
        
        // 更新面板文字
        root.findViewById<TextView>(R.id.clipboard_title)?.let {
            it.text = assetRes.getString("clipboard_title")
            it.setTextColor(keyText)
        }
        root.findViewById<Button>(R.id.btn_clear_clipboard)?.let {
            it.text = assetRes.getString("clipboard_clear")
            it.setTextColor(accentError)
        }

        // 填充编辑面板按钮文字
        root.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_select_all)?.text = assetRes.getString("edit_select_all")
        root.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_copy)?.text = assetRes.getString("edit_copy")
        root.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_pad_paste)?.text = assetRes.getString("edit_paste")
        root.findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_back_to_keyboard)?.text = assetRes.getString("edit_back")
    }

    private fun <T : View> findViewByIdInInputView(id: Int): T? {
        return inputRootView?.findViewById(id)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        when {
            key == SettingsManager.KEY_CURRENT_LANG || 
            key?.startsWith(SettingsManager.KEY_USE_PC_LAYOUT_PREFIX) == true ||
            key?.startsWith(SettingsManager.KEY_DICT_ENABLED_PREFIX) == true -> {
                reloadLanguage()
            }
            key == SettingsManager.KEY_HEIGHT_PERCENT || key == SettingsManager.KEY_CANDIDATE_HEIGHT || 
            key == SettingsManager.KEY_SWIPE_THRESHOLD || key == SettingsManager.KEY_H_SPACING || 
            key == SettingsManager.KEY_V_SPACING || key == SettingsManager.KEY_VIBRATION_ENABLED || 
            key == SettingsManager.KEY_VIBRATION_STRENGTH -> {
                applyKeyboardSettings()
            }
        }
    }

    override fun onPrimaryClipChanged() {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
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
        inputRootView = root
        keyboardView = root.findViewById(R.id.keyboard_view)
        candidateContainer = root.findViewById(R.id.candidate_container)
        btnClose = root.findViewById(R.id.btn_close_keyboard)
        toolbarView = root.findViewById(R.id.toolbar_view)
        candidateScroll = root.findViewById(R.id.candidate_scroll)
        candidateStrip = root.findViewById(R.id.candidate_strip)
        pageIndicator = root.findViewById(R.id.page_indicator)

        panelManager = PanelManager(root, { text ->
            currentInputConnection?.commitText(text, 1)
            updateCandidates()
        }, {
            // Back to keyboard callback if needed
        })

        setupToolbar(root)
        setupEditPad(root)
        
        root.findViewById<View>(R.id.root_container).setOnClickListener {
            if (settings.closeOutside) requestHideSelf(0)
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

    override fun onFinishInput() {
        super.onFinishInput()
        inputEngine.reset()
        updateCandidates()
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
        
        // 获取主题文字颜色用于指示器
        val textColorStr = keyboardView.theme?.keyText
        val textColor = try {
            if (textColorStr != null) android.graphics.Color.parseColor(textColorStr) else assetRes.getColor("key_text", android.graphics.Color.WHITE)
        } catch (e: Exception) {
            assetRes.getColor("key_text", android.graphics.Color.WHITE)
        }

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
                drawable.setColor(textColor)
                drawable.alpha = 255
            } else {
                drawable.setColor(textColor)
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
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                sendKeyWithActiveModifiers(keyCode)
                handler.postDelayed(this, 50)
            }
        }

        view.setOnTouchListener { v, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    sendKeyWithActiveModifiers(keyCode)
                    handler.postDelayed(runnable, 400)
                    v.isPressed = true
                }
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL -> {
                    handler.removeCallbacks(runnable)
                    v.isPressed = false
                }
            }
            true
        }
    }

    /**
     * 统一发送按键的方法，自动附带当前激活的修饰键状态（如 Shift, Ctrl 等）
     */
    private fun sendKeyWithActiveModifiers(keyCode: Int) {
        sendKey(keyCode, calculateTotalMetaState())
    }

    private fun reloadLanguage() {
        val langId = settings.currentLangId
        val isPcLayout = settings.usePcLayout(langId)
        val taskId = ++currentLoadTaskId

        // 切换到后台线程处理加载逻辑
        Thread {
            val config = LayoutManager.loadLanguageConfig(this, langId)
            mainHandler.post {
                if (taskId != currentLoadTaskId) return@post
                if (config != null) {
                    currentLanguage = config
                    val layoutFile = if (isPcLayout) config.pcLayout else config.mobileLayout
                    setupEngine(langId, isPcLayout)
                    loadLayout(langId, layoutFile)
                }
                updateCandidates()
            }
        }.start()
    }

    private fun setupEngine(langId: String, isPc: Boolean) {
        val config = currentLanguage ?: return
        val dictEnabled = settings.isDictEnabled(langId, isPc)
        
        inputEngine = if (config.engine == "dictionary" && dictEnabled) {
            DictionaryEngine(dictManager)
        } else {
            RawEngine()
        }
    }

    private fun loadLayout(langId: String, fileName: String) {
        val startTime = SystemClock.elapsedRealtime()
        val taskId = currentLoadTaskId
        
        Thread {
            val layout = LayoutManager.loadLayout(this, langId, fileName)
            mainHandler.post {
                if (taskId != currentLoadTaskId) return@post
                layout?.let { loadedLayout ->
                    currentLayout = loadedLayout
                    Log.d(TAG, "Layout loaded in ${SystemClock.elapsedRealtime() - startTime}ms")
                    
                    // 异步加载词典
                    if (inputEngine is DictionaryEngine) {
                        Thread {
                            currentLanguage?.dictionary?.let { dictFile ->
                                dictManager.loadDictionary(langId, dictFile)
                            }
                        }.start()
                    } else {
                        dictManager.clear()
                    }
                    inputEngine.reset()

                    keyboardView.theme = loadedLayout.theme
                    keyboardView.layoutThemeLight = loadedLayout.themeLight
                    keyboardView.layoutThemeDark = loadedLayout.themeDark
                    keyboardView.invalidate() // 强制应用新主题

                    if (::panelManager.isInitialized) {
                        panelManager.theme = loadedLayout.theme
                        panelManager.themeLight = loadedLayout.themeLight
                        panelManager.themeDark = loadedLayout.themeDark
                    }
                    candidateContainer.visibility = if (loadedLayout.showCandidates) View.VISIBLE else View.GONE
                    
                    activeStates.clear()
                    activeTransientKeyIds.clear()
                    metaStateMap.clear()
                    metaKeyCodeMap.clear()
                    
                    loadedLayout.pages.flatMap { it.rows }.flatMap { it.keys }.forEach { k ->
                        k.id?.let { id ->
                            activeStates[id] = false
                            k.metaValue?.let { meta -> metaStateMap[id] = meta }
                        }
                        k.parsedKeyCode = parseKeyCode(k.text)
                        if (k.type == KeyType.MODIFIER && k.id != null && k.parsedKeyCode != KeyEvent.KEYCODE_UNKNOWN) {
                            metaKeyCodeMap[k.id] = k.parsedKeyCode
                        }
                        k.overrides?.values?.forEach { override ->
                            override.parsedKeyCode = parseKeyCode(override.text)
                        }
                    }
                    keyboardView.activeStates = activeStates
                    val defaultPageId = currentLanguage?.defaultPage ?: "main"
                    val startPage = loadedLayout.pages.find { it.id == defaultPageId } ?: loadedLayout.pages.firstOrNull()
                    if (startPage != null) {
                        keyboardView.setPage(startPage)
                        updatePageIndicator(startPage)
                    }
                    applyThemeToStaticViews()
                }
            }
        }.start()
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

        // 1. 确定最终生效的输入内容 (考虑 overrides)，这一步提前，供引擎和后续逻辑共同使用
        var effInputJson = key.text
        var effKeyCode = key.parsedKeyCode
        key.overrides?.forEach { (stateId, override) ->
            if (activeStates[stateId] == true) {
                override.text?.let { 
                    effInputJson = it 
                    effKeyCode = override.parsedKeyCode
                }
            }
        }
        
        val effText = if (effInputJson?.isJsonPrimitive == true && effInputJson!!.asJsonPrimitive.isString) {
            effInputJson!!.asString
        } else null

        // 3. 状态切换逻辑 (完全数据驱动，基于 JSON 的 sticky 属性)
        if (key.sticky != null && key.id != null) {
            val currentState = activeStates[key.id] ?: false
            activeStates[key.id] = !currentState
            if (key.sticky == "transient") {
                if (!currentState) activeTransientKeyIds.add(key.id) else activeTransientKeyIds.remove(key.id)
            }
            keyboardView.activeStates = activeStates
            return
        }

        // 4. 计算组合键状态 (Meta State)
        val totalMeta = calculateTotalMetaState()

        // 5. 执行动作或发送输入
        if (key.action != null) {
            // 拦截逻辑：引擎也有权拦截动作（如 DictionaryEngine 拦截退格以同步删除联想词）
            if (!inputEngine.processKey(key, effText, effKeyCode, ic, totalMeta)) {
                handleAction(key.action)
            }
            
            // 修复换页重置 Bug：如果是换页动作，不执行后续的瞬时状态重置
            if (key.action.startsWith(ACTION_SWITCH_PAGE)) {
                updateCandidates()
                return
            }
        } else {
            // 拦截逻辑：如果引擎处理了此输入（例如加入联想序列），则不再执行默认分发
            if (!inputEngine.processKey(key, effText, effKeyCode, ic, totalMeta)) {
                dispatchInput(effKeyCode, effInputJson, totalMeta, ic)
            }
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
        when {
            action.startsWith(ACTION_SWITCH_PAGE) -> {
                val pageId = action.substringAfter(ACTION_SWITCH_PAGE)
                currentLayout?.pages?.find { it.id == pageId }?.let { 
                    keyboardView.setPage(it)
                    updatePageIndicator(it)
                }
            }
            // 修复：退格键应通过 KeyCode 发送，以正确处理选中文本的删除逻辑
            action == "backspace" -> sendKey(KeyEvent.KEYCODE_DEL, 0)
            action == "hide" -> requestHideSelf(0)
        }
    }

    private fun sendKey(code: Int, meta: Int) {
        val ic = currentInputConnection ?: return
        val now = SystemClock.uptimeMillis()
        
        // 1. 模拟物理修饰键按下 (基于当前激活的状态 ID 自动查找物理按键)
        val activeModifiers = mutableListOf<Int>()
        activeStates.forEach { (id, isActive) ->
            if (isActive) {
                metaKeyCodeMap[id]?.let { activeModifiers.add(it) }
            }
        }

        activeModifiers.forEach { modCode ->
            ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, modCode, 0, 0))
        }

        // 2. 发送目标按键
        ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, code, 0, meta))
        ic.sendKeyEvent(KeyEvent(now, now + 10, KeyEvent.ACTION_UP, code, 0, meta))

        // 3. 模拟物理修饰键抬起
        activeModifiers.forEach { modCode ->
            ic.sendKeyEvent(KeyEvent(now, now + 20, KeyEvent.ACTION_UP, modCode, 0, 0))
        }
    }

    private fun updateCandidates() {
        if (!::candidateStrip.isInitialized) return
        val candidates = inputEngine.getCandidates()

        if (candidates.isEmpty()) {
            toolbarView.visibility = View.VISIBLE
            candidateScroll.visibility = View.GONE
            return
        }

        toolbarView.visibility = View.GONE
        candidateScroll.visibility = View.VISIBLE
        
        val textColorStr = keyboardView.theme?.keyText
        val textColor = try {
            if (textColorStr != null) android.graphics.Color.parseColor(textColorStr) else assetRes.getColor("key_text", android.graphics.Color.WHITE)
        } catch (e: Exception) {
            assetRes.getColor("key_text", android.graphics.Color.WHITE)
        }

        val textSize = settings.candidateTextSize
        val padding = settings.candidatePadding

        // 视图复用逻辑
        val currentChildCount = candidateStrip.childCount
        for (i in 0 until maxOf(candidates.size, currentChildCount)) {
            if (i < candidates.size) {
                val candidate = candidates[i]
                val tv = if (i < currentChildCount) {
                    candidateStrip.getChildAt(i) as TextView
                } else {
                    val newTv = TextView(this)
                    newTv.gravity = android.view.Gravity.CENTER
                    newTv.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    newTv.setBackgroundResource(android.R.drawable.list_selector_background)
                    candidateStrip.addView(newTv)
                    newTv
                }
                tv.visibility = View.VISIBLE
                tv.text = candidate
                tv.setTextColor(textColor)
                tv.textSize = textSize
                tv.setPadding(padding, 0, padding, 0)
                tv.setOnClickListener {
                    currentInputConnection?.commitText(candidate, 1)
                    inputEngine.processKey(KeyboardKey(action = "commit"), null, KeyEvent.KEYCODE_UNKNOWN, currentInputConnection!!, calculateTotalMetaState())
                    resetTransientStates()
                    updateCandidates()
                }
            } else {
                // 隐藏多余的视图
                candidateStrip.getChildAt(i).visibility = View.GONE
            }
        }
    }

    private fun applyKeyboardSettings() {
        if (!::keyboardView.isInitialized) return
        keyboardView.layoutParams.height = (resources.displayMetrics.heightPixels * (settings.heightPercent / 100f)).toInt()
        keyboardView.swipeThreshold = settings.swipeThreshold
        keyboardView.horizontalSpacing = settings.horizontalSpacing
        keyboardView.verticalSpacing = settings.verticalSpacing
        keyboardView.vibrationEnabled = settings.vibrationEnabled
        keyboardView.vibrationStrength = settings.vibrationStrength
        
        // New appearance/animation settings
        keyboardView.keyRadius = settings.keyRadius
        keyboardView.funcKeyRadius = settings.funcKeyRadius
        keyboardView.shadowOffset = settings.shadowOffset
        keyboardView.shadowAlpha = settings.shadowAlpha
        keyboardView.animDuration = settings.animDuration
        keyboardView.rippleExpandDuration = settings.rippleDuration
        
        candidateContainer.layoutParams.height = (settings.candidateHeight * resources.displayMetrics.density).toInt()
        keyboardView.requestLayout(); candidateContainer.requestLayout()
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) { 
        super.onStartInputView(info, restarting)
        applyKeyboardSettings()
        inputEngine.reset()
        updateCandidates()
        
        // 确保每次打开都显示键盘面板而非剪贴板/编辑面板
        if (::panelManager.isInitialized) {
            panelManager.switchPanel(PanelManager.PanelType.KEYBOARD)
        }
        
        // 自动适配输入框类型：如果是数字或电话，自动切换到 symbols 页面
        info?.let {
            val inputType = it.inputType and EditorInfo.TYPE_MASK_CLASS
            val isNumber = inputType == EditorInfo.TYPE_CLASS_NUMBER || inputType == EditorInfo.TYPE_CLASS_PHONE
            val defaultPageId = currentLanguage?.defaultPage ?: "main"
            val targetPageId = if (isNumber) "symbols" else defaultPageId
            
            currentLayout?.pages?.find { p -> p.id == targetPageId }?.let { page ->
                keyboardView.setPage(page)
                updatePageIndicator(page)
            }
        }
    }
}