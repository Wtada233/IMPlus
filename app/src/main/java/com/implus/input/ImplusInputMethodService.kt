@file:Suppress("TooManyFunctions")
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
import com.implus.input.engine.InputEngine
import com.implus.input.engine.RawEngine
import com.implus.input.engine.DictionaryEngine
import com.implus.input.layout.KeyboardLayout
import com.implus.input.layout.LanguageConfig
import com.implus.input.layout.KeyboardPage
import com.implus.input.layout.KeyType
import com.implus.input.layout.KeyboardKey
import com.implus.input.layout.LayoutManager
import com.implus.input.manager.SettingsManager
import com.implus.input.manager.AssetResourceManager
import com.implus.input.manager.DictionaryManager
import com.implus.input.manager.UIManager
import com.implus.input.manager.PanelManager
import com.implus.input.manager.ClipboardHistoryManager
import com.implus.input.manager.KeyboardStateManager
import com.implus.input.logic.ActionHandler
import com.implus.input.utils.Constants

class ImplusInputMethodService : InputMethodService(), android.content.ClipboardManager.OnPrimaryClipChangedListener, SharedPreferences.OnSharedPreferenceChangeListener {

    companion object {
        private const val TAG = "ImplusInputMethodService"
        private const val INDICATOR_DOT_SIZE_DP = 6
        private const val INDICATOR_DOT_MARGIN_DP = 4
    }

    private lateinit var keyboardView: ImplusKeyboardView
    private lateinit var candidateContainer: RelativeLayout
    private lateinit var btnClose: ImageView
    private var currentLayout: KeyboardLayout? = null
    private var currentLanguage: LanguageConfig? = null
    private var inputEngine: InputEngine = RawEngine()
    private val dictManager by lazy { DictionaryManager(this) }
    private val uiManager by lazy { UIManager(assetRes) }
    private val stateManager = KeyboardStateManager()
    private val actionHandler by lazy {
        ActionHandler(
            sendKeyFunc = { code, meta -> sendKey(code, meta) },
            hideFunc = { requestHideSelf(0) },
            switchPageFunc = { page -> 
                keyboardView.setPage(page)
                updatePageIndicator(page)
            }
        )
    }
    
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val loadExecutor = java.util.concurrent.Executors.newSingleThreadExecutor()
    private var currentLoadTaskId = 0
    
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
        loadExecutor.shutdownNow()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        assetRes.refresh()
        uiManager.applyThemeToStaticViews(inputRootView)
    }

    private fun applyThemeToStaticViews() {
        uiManager.applyThemeToStaticViews(inputRootView)
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
        
        initializeViews(root)
        setupPanelManager(root)
        setupToolbar(root)
        setupEditPad(root)
        setupRootListeners(root)

        reloadLanguage()
        applyKeyboardSettings()

        setupKeyboardLogic()
        
        return root
    }

    private fun initializeViews(root: View) {
        keyboardView = root.findViewById(R.id.keyboard_view)
        candidateContainer = root.findViewById(R.id.candidate_container)
        btnClose = root.findViewById(R.id.btn_close_keyboard)
        toolbarView = root.findViewById(R.id.toolbar_view)
        candidateScroll = root.findViewById(R.id.candidate_scroll)
        candidateStrip = root.findViewById(R.id.candidate_strip)
        pageIndicator = root.findViewById(R.id.page_indicator)
    }

    private fun setupPanelManager(root: View) {
        panelManager = PanelManager(root) { text ->
            currentInputConnection?.commitText(text, Constants.DEFAULT_CURSOR_OFFSET)
            updateCandidates()
        }
    }

    private fun setupRootListeners(root: View) {
        root.findViewById<View>(R.id.root_container).setOnClickListener {
            val hideFlags = 0
            if (settings.closeOutside) requestHideSelf(hideFlags)
        }
        root.findViewById<View>(R.id.keyboard_view).parent.let { (it as View).setOnClickListener { } }
        val closeHideFlags = 0
        btnClose.setOnClickListener { requestHideSelf(closeHideFlags) }
    }

    private fun setupKeyboardLogic() {
        keyboardView.onKeyListener = { key -> handleKey(key) }
        keyboardView.onSwipeListener = { direction -> handleSwipe(direction) }
    }

    private fun handleSwipe(direction: ImplusKeyboardView.Direction) {
        val allPages = currentLayout?.pages ?: emptyList()
        val currentId = keyboardView.getCurrentPageId()
        val currentPage = allPages.find { it.id == currentId } ?: return
        
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
        val textColor = parseIndicatorColor(textColorStr)

        for (i in groupPages.indices) {
            val dot = View(this)
            val density = resources.displayMetrics.density
            val size = (INDICATOR_DOT_SIZE_DP * density).toInt()
            val margin = (INDICATOR_DOT_MARGIN_DP * density).toInt()
            val params = android.widget.LinearLayout.LayoutParams(size, size)
            params.setMargins(margin, 0, margin, 0)
            dot.layoutParams = params
            
            val drawable = android.graphics.drawable.GradientDrawable()
            drawable.shape = android.graphics.drawable.GradientDrawable.OVAL
            drawable.setColor(textColor)
            drawable.alpha = if (i == currentIndex) Constants.ALPHA_OPAQUE else Constants.ALPHA_SEMI_TRANSPARENT
            dot.background = drawable
            pageIndicator.addView(dot)
        }
    }

    private fun parseIndicatorColor(textColorStr: String?): Int {
        return try {
            if (textColorStr != null) android.graphics.Color.parseColor(textColorStr) 
            else assetRes.getColor("key_text", android.graphics.Color.WHITE)
        } catch (e: IllegalArgumentException) {
            android.util.Log.e(TAG, "Indicator color error", e)
            assetRes.getColor("key_text", android.graphics.Color.WHITE)
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
                handler.postDelayed(this, Constants.REPEAT_DELAY_MS)
            }
        }

        view.setOnTouchListener { v, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    sendKeyWithActiveModifiers(keyCode)
                    handler.postDelayed(runnable, Constants.INITIAL_REPEAT_DELAY_MS)
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

        // 切换到后台线程处理加载逻辑 (串行执行)
        loadExecutor.execute {
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
        }
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
        
        loadExecutor.execute {
            val layout = LayoutManager.loadLayout(this, langId, fileName)
            mainHandler.post {
                if (taskId != currentLoadTaskId) return@post
                layout?.let { loadedLayout ->
                    onLayoutLoaded(loadedLayout, startTime)
                }
            }
        }
    }

    private fun onLayoutLoaded(loadedLayout: KeyboardLayout, startTime: Long) {
        currentLayout = loadedLayout
        Log.d(TAG, "Layout loaded in ${SystemClock.elapsedRealtime() - startTime}ms")
        
        // 异步加载词典 (放入同一队列以确保顺序，或独立执行均可，这里为了不阻塞后续UI操作选择独立execute)
        if (inputEngine is DictionaryEngine) {
            loadExecutor.execute {
                currentLanguage?.dictionary?.let { dictFile ->
                    dictManager.loadDictionary(currentLanguage?.id ?: "", dictFile)
                }
            }
        } else {
            dictManager.clear()
        }
        inputEngine.reset()

        applyLayoutToViews(loadedLayout)
        resetStateAndPreparseKeys(loadedLayout)
        
        val defaultPageId = currentLanguage?.defaultPage ?: "main"
        val startPage = loadedLayout.pages.find { it.id == defaultPageId } ?: loadedLayout.pages.firstOrNull()
        if (startPage != null) {
            keyboardView.setPage(startPage)
            updatePageIndicator(startPage)
        }
        applyThemeToStaticViews()
    }

    private fun applyLayoutToViews(layout: KeyboardLayout) {
        keyboardView.theme = layout.theme
        keyboardView.layoutThemeLight = layout.themeLight
        keyboardView.layoutThemeDark = layout.themeDark
        keyboardView.invalidate() // 强制应用新主题

        if (::panelManager.isInitialized) {
            panelManager.theme = layout.theme
            panelManager.themeLight = layout.themeLight
            panelManager.themeDark = layout.themeDark
        }
        candidateContainer.visibility = if (layout.showCandidates) View.VISIBLE else View.GONE
    }

    private fun resetStateAndPreparseKeys(layout: KeyboardLayout) {
        stateManager.resetAndPreparse(layout) { parseKeyCode(it) }
        keyboardView.activeStates = stateManager.activeStates
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

    private data class EffectiveInput(val text: String?, val keyCode: Int, val json: com.google.gson.JsonElement?)

    private fun handleKey(key: KeyboardKey) {
        val ic = currentInputConnection ?: return
        val effective = resolveEffectiveInput(key)
        
        var shouldPostProcess = true
        if (handleStickyKey(key)) {
            shouldPostProcess = false
        }

        if (shouldPostProcess) {
            val totalMeta = stateManager.calculateTotalMetaState()
            processInputOrAction(key, effective, ic, totalMeta)
            
            if (stateManager.resetTransientStates()) {
                keyboardView.activeStates = stateManager.activeStates
            }
            updateCandidates()
        }
    }

    private fun processInputOrAction(
        key: KeyboardKey, 
        effective: EffectiveInput, 
        ic: android.view.inputmethod.InputConnection, 
        totalMeta: Int
    ) {
        if (key.action != null) {
            if (!inputEngine.processKey(key, effective.text, effective.keyCode, ic, totalMeta)) {
                actionHandler.handleAction(key.action, currentLayout)
            }
        } else {
            if (!inputEngine.processKey(key, effective.text, effective.keyCode, ic, totalMeta)) {
                actionHandler.dispatchInput(effective.keyCode, effective.json, totalMeta, ic) { 
                    c, m -> sendKey(c, m) 
                }
            }
        }
    }

    private fun resolveEffectiveInput(key: KeyboardKey): EffectiveInput {
        var effInputJson = key.text
        var effKeyCode = key.parsedKeyCode
        key.overrides?.forEach { (stateId, override) ->
            if (stateManager.activeStates[stateId] == true) {
                override.text?.let { 
                    effInputJson = it 
                    effKeyCode = override.parsedKeyCode
                }
            }
        }
        val effText = if (effInputJson?.isJsonPrimitive == true && effInputJson!!.asJsonPrimitive.isString) {
            effInputJson!!.asString
        } else null
        return EffectiveInput(effText, effKeyCode, effInputJson)
    }

    private fun handleStickyKey(key: KeyboardKey): Boolean {
        if (key.sticky != null && key.id != null) {
            val currentState = stateManager.activeStates[key.id] ?: false
            stateManager.activeStates[key.id] = !currentState
            if (key.sticky == "transient") {
                if (!currentState) stateManager.activeTransientKeyIds.add(key.id) 
                else stateManager.activeTransientKeyIds.remove(key.id)
            }
            keyboardView.activeStates = stateManager.activeStates
            return true
        }
        return false
    }

    private fun calculateTotalMetaState(): Int = stateManager.calculateTotalMetaState()

    private fun resetTransientStates() {
        if (stateManager.resetTransientStates()) {
            keyboardView.activeStates = stateManager.activeStates
        }
    }

    private fun sendKey(code: Int, meta: Int) {
        val ic = currentInputConnection ?: return
        val now = SystemClock.uptimeMillis()
        
        val activeModifiers = mutableListOf<Int>()
        stateManager.activeStates.forEach { (id, isActive) ->
            if (isActive) {
                stateManager.metaKeyCodeMap[id]?.let { activeModifiers.add(it) }
            }
        }

        activeModifiers.forEach { modCode ->
            ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, modCode, 0, 0))
        }

        ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_DOWN, code, 0, meta))
        ic.sendKeyEvent(KeyEvent(now, now + Constants.PRESS_DELAY_MS, KeyEvent.ACTION_UP, code, 0, meta))

        activeModifiers.forEach { modCode ->
            ic.sendKeyEvent(KeyEvent(now, now + Constants.RELEASE_DELAY_MS, KeyEvent.ACTION_UP, modCode, 0, 0))
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
        val textColor = parseCandidateColor(textColorStr)

        renderCandidateViews(candidates, textColor)
    }

    private fun parseCandidateColor(colorStr: String?): Int {
        return try {
            if (colorStr != null) android.graphics.Color.parseColor(colorStr) 
            else assetRes.getColor("key_text", android.graphics.Color.WHITE)
        } catch (e: IllegalArgumentException) {
            android.util.Log.e(TAG, "Candidate color error", e)
            assetRes.getColor("key_text", android.graphics.Color.WHITE)
        }
    }

    private fun renderCandidateViews(candidates: List<String>, textColor: Int) {
        val textSize = settings.candidateTextSize
        val padding = settings.candidatePadding
        val currentChildCount = candidateStrip.childCount

        for (i in 0 until maxOf(candidates.size, currentChildCount)) {
            if (i < candidates.size) {
                val candidate = candidates[i]
                val tv = getOrCreateCandidateTextView(i, currentChildCount)
                tv.visibility = View.VISIBLE
                tv.text = candidate
                tv.setTextColor(textColor)
                tv.textSize = textSize
                tv.setPadding(padding, 0, padding, 0)
                tv.setOnClickListener {
                    onCandidateClicked(candidate)
                }
            } else {
                candidateStrip.getChildAt(i).visibility = View.GONE
            }
        }
    }

    private fun getOrCreateCandidateTextView(index: Int, currentCount: Int): TextView {
        return if (index < currentCount) {
            candidateStrip.getChildAt(index) as TextView
        } else {
            TextView(this).apply {
                gravity = android.view.Gravity.CENTER
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, 
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                setBackgroundResource(android.R.drawable.list_selector_background)
                candidateStrip.addView(this)
            }
        }
    }

    private fun onCandidateClicked(candidate: String) {
        currentInputConnection?.commitText(candidate, 1)
        val commitKey = KeyboardKey(action = "commit")
        inputEngine.processKey(
            commitKey, 
            null, 
            KeyEvent.KEYCODE_UNKNOWN, 
            currentInputConnection!!, 
            calculateTotalMetaState()
        )
        resetTransientStates()
        updateCandidates()
    }

    private fun applyKeyboardSettings() {
        if (!::keyboardView.isInitialized) return
        val heightRatio = settings.heightPercent / Constants.MAX_PERCENT.toFloat()
        keyboardView.layoutParams.height = (resources.displayMetrics.heightPixels * heightRatio).toInt()
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
        
        // 数据驱动的输入类型适配逻辑
        info?.let {
            val targetPageId = resolveTargetPageId(it)
            currentLayout?.pages?.find { p -> p.id == targetPageId }?.let { page ->
                keyboardView.setPage(page)
                updatePageIndicator(page)
            }
        }
    }

    private fun resolveTargetPageId(info: EditorInfo): String {
        val config = currentLanguage ?: return "main"
        val inputClass = info.inputType and EditorInfo.TYPE_MASK_CLASS
        
        // 将 Android 输入类型映射为语义化的 key
        val typeKey = when (inputClass) {
            EditorInfo.TYPE_CLASS_NUMBER -> "number"
            EditorInfo.TYPE_CLASS_PHONE -> "phone"
            EditorInfo.TYPE_CLASS_DATETIME -> "datetime"
            else -> "text"
        }
        
        // 优先从 JSON 配置中获取，没有则使用默认页面
        return config.inputTypePages?.get(typeKey) ?: config.defaultPage
    }
}

