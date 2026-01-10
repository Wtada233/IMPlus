package com.implus.input.engine

import android.view.KeyEvent
import android.view.inputmethod.InputConnection
import com.implus.input.layout.KeyboardKey
import com.implus.input.manager.DictionaryManager

interface InputEngine {
    fun processKey(key: KeyboardKey, effText: String?, effKeyCode: Int, ic: InputConnection, metaState: Int): Boolean
    fun getCandidates(): List<String>
    fun reset()
}

class RawEngine : InputEngine {
    override fun processKey(key: KeyboardKey, effText: String?, effKeyCode: Int, ic: InputConnection, metaState: Int): Boolean {
        return false
    }

    override fun getCandidates(): List<String> = emptyList()
    override fun reset() {}
}

class DictionaryEngine(private val dictManager: DictionaryManager) : InputEngine {
    private var composingText = ""
    private var candidates = listOf<String>()

    override fun processKey(key: KeyboardKey, effText: String?, effKeyCode: Int, ic: InputConnection, metaState: Int): Boolean {
        if (key.action == "commit") {
            reset()
            return true
        }

        if (key.action == "backspace") {
            if (composingText.isNotEmpty()) {
                composingText = composingText.substring(0, composingText.length - 1)
                updateCandidates(ic)
                return true 
            }
            reset() // 如果退格导致前缀为空，重置引擎状态
            return false
        }

        // 只有在非 Action 按键且有文本输入时才加入联想
        if (key.action == null && effText != null && effText.length == 1 && !effText[0].isWhitespace()) {
            composingText += effText
            updateCandidates(ic)
            return true
        }

        // 其他按键（如回车、空格等）触发提交
        commitComposing(ic)
        return false
    }

    private fun commitComposing(ic: InputConnection?) {
        if (ic == null) return
        if (composingText.isNotEmpty()) {
            ic.commitText(composingText, 1)
            ic.setComposingText("", 1)
            reset()
        }
    }

    private fun updateCandidates(ic: InputConnection?) {
        if (ic == null) {
            candidates = emptyList()
            return
        }
        
        if (composingText.isNotEmpty()) {
            try {
                ic.setComposingText(composingText, 1)
                val suggestions = dictManager.getSuggestions(composingText)
                
                // 核心拦截逻辑：保留用户输入的原始前缀，仅附加词典的剩余部分
                candidates = suggestions.map { word ->
                    if (word.lowercase().startsWith(composingText.lowercase())) {
                        composingText + word.substring(composingText.length)
                    } else {
                        word
                    }
                }
                
                if (candidates.none { it.equals(composingText, ignoreCase = true) }) {
                    candidates = listOf(composingText) + candidates
                }
            } catch (e: Exception) {
                candidates = listOf(composingText)
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

