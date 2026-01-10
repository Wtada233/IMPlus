package com.implus.input.engine

import android.view.KeyEvent
import android.view.inputmethod.InputConnection
import com.implus.input.layout.KeyboardKey
import com.implus.input.manager.DictionaryManager

interface InputEngine {
    var enabled: Boolean
    fun processKey(key: KeyboardKey, ic: InputConnection, activeStates: Map<String, Boolean>): Boolean
    fun getCandidates(): List<String>
    fun reset()
}

class RawEngine : InputEngine {
    override var enabled: Boolean = true
    override fun processKey(key: KeyboardKey, ic: InputConnection, activeStates: Map<String, Boolean>): Boolean {
        // RawEngine 不拦截按键，交给 Service 处理标准输出
        return false
    }

    override fun getCandidates(): List<String> = emptyList()
    override fun reset() {}
}

class DictionaryEngine(private val dictManager: DictionaryManager) : InputEngine {
    override var enabled: Boolean = true
    private var composingText = ""
    private var candidates = listOf<String>()

    override fun processKey(key: KeyboardKey, ic: InputConnection, activeStates: Map<String, Boolean>): Boolean {
        if (!enabled) return false

        // 如果是特殊的 commit 动作（点击候选词后）
        if (key.action == "commit") {
            reset()
            return true
        }

        // 处理退格
        if (key.action == "backspace") {
            if (composingText.isNotEmpty()) {
                composingText = composingText.substring(0, composingText.length - 1)
                updateCandidates(ic)
                return composingText.isNotEmpty() // 如果还有内容，拦截退格不让系统删文字
            }
            return false
        }

        // 确定最终生效的输入内容和 KeyCode
        var effText: String? = null
        if (key.text?.isJsonPrimitive == true && key.text.asJsonPrimitive.isString) {
            effText = key.text.asString
        }
        var effKeyCode = key.parsedKeyCode

        key.overrides?.forEach { (id, override) ->
            if (activeStates[id] == true) {
                override.text?.let { 
                    effText = it.asString
                    effKeyCode = override.parsedKeyCode
                }
            }
        }

        // 如果是字母键 A-Z
        if (effKeyCode >= KeyEvent.KEYCODE_A && effKeyCode <= KeyEvent.KEYCODE_Z) {
            val isShift = activeStates["shift"] == true
            val isCaps = activeStates["caps"] == true
            val isUpper = isShift xor isCaps
            
            val baseChar = 'a' + (effKeyCode - KeyEvent.KEYCODE_A)
            val char = if (isUpper) baseChar.uppercaseChar() else baseChar
            
            composingText += char
            updateCandidates(ic)
            return true
        } 
        
        // 处理其他单字符输入（如数字、符号）
        if (effText != null && effText!!.length == 1) {
            val char = effText!![0]
            if (char.isLetterOrDigit()) {
                composingText += char
                updateCandidates(ic)
                return true
            }
        }

        // 如果按下了非组合键（如空格、回车），提交当前内容并重置
        if (composingText.isNotEmpty()) {
            ic.commitText(composingText, 1)
            reset()
            ic.setComposingText("", 1)
        }
        return false
    }

    private fun updateCandidates(ic: InputConnection) {
        if (composingText.isNotEmpty()) {
            ic.setComposingText(composingText, 1)
            candidates = dictManager.getSuggestions(composingText)
            // 如果词典里没找到，或者输入太短，我们也把当前输入作为一个候选词
            if (!candidates.contains(composingText)) {
                candidates = listOf(composingText) + candidates
            }
        } else {
            candidates = emptyList()
            ic.setComposingText("", 1)
        }
    }

    override fun getCandidates(): List<String> = candidates

    override fun reset() {
        composingText = ""
        candidates = emptyList()
    }
}

