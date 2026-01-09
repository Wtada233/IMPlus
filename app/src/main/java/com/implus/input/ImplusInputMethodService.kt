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
    private lateinit var editPadView: View
    private lateinit var clipboardView: View
    private lateinit var clipboardList: ViewGroup
    private lateinit var pageIndicator: android.widget.LinearLayout

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
        val root = layoutInflater.inflate(R.layout.input_view, null)
        keyboardView = root.findViewById(R.id.keyboard_view)
        candidateContainer = root.findViewById(R.id.candidate_container)
        btnClose = root.findViewById(R.id.btn_close_keyboard)
        toolbarView = root.findViewById(R.id.toolbar_view)
        candidateScroll = root.findViewById(R.id.candidate_scroll)
        editPadView = root.findViewById(R.id.edit_pad_view)
        clipboardView = root.findViewById(R.id.clipboard_view)
        clipboardList = root.findViewById(R.id.clipboard_list)
        pageIndicator = root.findViewById(R.id.page_indicator)

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
                // Filter pages by group ID. If no group ID, only include pages with no group ID (or just itself)
                // Here: If groupId is null, it's isolated (like 'main'). If set, it groups with others.
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
            switchPanel(PanelType.EDIT)
        }
        root.findViewById<View>(R.id.btn_clipboard).setOnClickListener {
            if (clipboardView.visibility == View.VISIBLE) {
                switchPanel(PanelType.KEYBOARD)
            } else {
                showClipboardHistory()
            }
        }
    }

    enum class PanelType { KEYBOARD, EDIT, CLIPBOARD }

    private fun switchPanel(type: PanelType) {
        keyboardView.visibility = if (type == PanelType.KEYBOARD) View.VISIBLE else View.GONE
        editPadView.visibility = if (type == PanelType.EDIT) View.VISIBLE else View.GONE
        clipboardView.visibility = if (type == PanelType.CLIPBOARD) View.VISIBLE else View.GONE
    }

    private fun showClipboardHistory() {
        switchPanel(PanelType.CLIPBOARD)
        
        clipboardList.removeAllViews()
        val history = ClipboardHistoryManager.getHistory(this)
        
        if (history.isEmpty()) {
            val emptyView = TextView(this)
            emptyView.text = getString(R.string.clipboard_empty)
            emptyView.setTextColor(android.graphics.Color.WHITE)
            emptyView.setPadding(32, 32, 32, 32)
            clipboardList.addView(emptyView)
        } else {
            for (text in history) {
                val tv = TextView(this)
                tv.text = text
                tv.setTextColor(android.graphics.Color.WHITE)
                tv.textSize = 16f
                tv.setPadding(32, 24, 32, 24)
                tv.setBackgroundResource(android.R.drawable.list_selector_background)
                tv.setOnClickListener {
                    currentInputConnection?.commitText(text, 1)
                    switchPanel(PanelType.KEYBOARD)
                }
                clipboardList.addView(tv)
                
                val divider = View(this)
                divider.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1)
                divider.setBackgroundColor(getColor(R.color.divider_dark))
                clipboardList.addView(divider)
            }
        }
    }

    private fun setupEditPad(root: View) {
        // Removed unused variable 'ic'
        root.findViewById<View>(R.id.btn_select_all).setOnClickListener {
            currentInputConnection?.performContextMenuAction(android.R.id.selectAll)
        }
        root.findViewById<View>(R.id.btn_copy).setOnClickListener {
            currentInputConnection?.performContextMenuAction(android.R.id.copy)
        }
        root.findViewById<View>(R.id.btn_pad_paste).setOnClickListener {
            currentInputConnection?.performContextMenuAction(android.R.id.paste)
        }
        
        root.findViewById<View>(R.id.btn_arrow_left).setOnClickListener { sendKey(KeyEvent.KEYCODE_DPAD_LEFT, 0) }
        root.findViewById<View>(R.id.btn_arrow_right).setOnClickListener { sendKey(KeyEvent.KEYCODE_DPAD_RIGHT, 0) }
        root.findViewById<View>(R.id.btn_arrow_up).setOnClickListener { sendKey(KeyEvent.KEYCODE_DPAD_UP, 0) }
        root.findViewById<View>(R.id.btn_arrow_down).setOnClickListener { sendKey(KeyEvent.KEYCODE_DPAD_DOWN, 0) }
        
        root.findViewById<View>(R.id.btn_back_to_keyboard).setOnClickListener {
            switchPanel(PanelType.KEYBOARD)
        }
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
            }
            keyboardView.activeStates = activeStates
            val mainPage = layout.pages.find { it.id == "main" }
            if (mainPage != null) {
                keyboardView.setPage(mainPage)
                updatePageIndicator(mainPage)
            }
        }
    }

    private fun handleKey(key: KeyboardKey) {
        val ic = currentInputConnection ?: return

        if (inputEngine.processKey(key, ic, activeStates)) {
            updateCandidates()
            return
        }
        
        if (key.sticky != null && key.id != null) {
            val newState = !(activeStates[key.id] ?: false)
            activeStates[key.id] = newState
            if (key.sticky == "transient") {
                if (newState) activeTransientKeyIds.add(key.id) else activeTransientKeyIds.remove(key.id)
            }
            keyboardView.activeStates = activeStates
            return
        }

        var effInput = key.text
        key.overrides?.forEach { (stateId, override) ->
            if (activeStates[stateId] == true) {
                override.text?.let { effInput = it }
            }
        }

        // Optimized Meta State Calculation: 使用系统常量并自动补充掩码以提高兼容性
        var totalMeta = 0
        activeStates.forEach { (stateId, isActive) ->
            if (isActive) {
                metaStateMap[stateId]?.let { meta ->
                    totalMeta = totalMeta or meta
                    // 兼容性补充：如果开启了 Ctrl/Alt/Shift，同时开启对应的 LEFT 标志
                    when (meta) {
                        KeyEvent.META_CTRL_ON -> totalMeta = totalMeta or KeyEvent.META_CTRL_LEFT_ON
                        KeyEvent.META_ALT_ON -> totalMeta = totalMeta or KeyEvent.META_ALT_LEFT_ON
                        KeyEvent.META_SHIFT_ON -> totalMeta = totalMeta or KeyEvent.META_SHIFT_LEFT_ON
                    }
                }
            }
        }

        if (key.action != null) {
            handleAction(key.action)
        } else {
            effInput?.let { input ->
                if (input.isJsonPrimitive) {
                    val p = input.asJsonPrimitive
                    if (p.isNumber) {
                        sendKey(p.asInt, totalMeta)
                    } else if (p.isString) {
                        val str = p.asString
                        // 1. 尝试作为 KeyCode 处理 (支持 "A", "ENTER", "DEL", "COMMA" 等)
                        val code = KeyEvent.keyCodeFromString("KEYCODE_${str.uppercase()}")
                        if (code != KeyEvent.KEYCODE_UNKNOWN) {
                            sendKey(code, totalMeta)
                        } else {
                            // 2. 作为字面量提交 (如 " ", "😊", "hello")
                            ic.commitText(str, 1)
                        }
                    }
                }
            }
        }

        // 优化：仅针对当前激活的 transient 键进行重置，避免 O(N) 遍历
        if (activeTransientKeyIds.isNotEmpty()) {
            activeTransientKeyIds.forEach { id -> activeStates[id] = false }
            activeTransientKeyIds.clear()
            keyboardView.activeStates = activeStates
        }
        
        updateCandidates()
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
        ic.sendKeyEvent(KeyEvent(now, now, KeyEvent.ACTION_UP, code, 0, meta))
    }

    private fun updateCandidates() {
        val candidates = inputEngine.getCandidates()
        if (candidates.isEmpty()) {
            toolbarView.visibility = View.VISIBLE
            candidateScroll.visibility = View.GONE
        } else {
            toolbarView.visibility = View.GONE
            candidateScroll.visibility = View.VISIBLE
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
