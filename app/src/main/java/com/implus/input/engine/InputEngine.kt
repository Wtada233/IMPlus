package com.implus.input.engine

import android.view.KeyEvent
import android.view.inputmethod.InputConnection
import com.implus.input.layout.KeyboardKey
import com.implus.input.manager.DictionaryManager

interface InputEngine {
    fun processKey(key: KeyboardKey, ic: InputConnection, metaState: Int): Boolean
    fun getCandidates(): List<String>
    fun reset()
}

class RawEngine : InputEngine {
    override fun processKey(key: KeyboardKey, ic: InputConnection, metaState: Int): Boolean {
        return false
    }

    override fun getCandidates(): List<String> = emptyList()
    override fun reset() {}
}

class DictionaryEngine(private val dictManager: DictionaryManager) : InputEngine {
    private var composingText = ""
    private var candidates = listOf<String>()

    override fun processKey(key: KeyboardKey, ic: InputConnection, metaState: Int): Boolean {
        if (key.action == "commit") {
            reset()
            return true
        }

        if (key.action == "backspace") {
            if (composingText.isNotEmpty()) {
                composingText = composingText.substring(0, composingText.length - 1)
                updateCandidates(ic)
                return composingText.isNotEmpty()
            }
            return false
        }

        var effText: String? = null
        if (key.text?.isJsonPrimitive == true && key.text.asJsonPrimitive.isString) {
            effText = key.text.asString
        }
        
        // 注意：此处不再处理 overrides，因为 Service 已经计算好了最终的 effText 和 keyCode
        // 我们直接使用传入的 key 属性即可

        if (effText != null && effText.length == 1) {
            val char = effText[0]
            if (char.isLetter()) {
                // 使用 Android 标准的 metaState 判断大小写
                val isShift = (metaState and android.view.KeyEvent.META_SHIFT_ON) != 0
                val isCaps = (metaState and android.view.KeyEvent.META_CAPS_LOCK_ON) != 0
                val finalChar = if (isShift xor isCaps) char.uppercaseChar() else char.lowercaseChar()
                
                composingText += finalChar
                updateCandidates(ic)
                return true
            } else if (char.isDigit() || char.isWhitespace()) {
                commitComposing(ic)
                return false
            }
        }

        commitComposing(ic)
        return false
    }

    private fun commitComposing(ic: InputConnection) {
        if (composingText.isNotEmpty()) {
            ic.commitText(composingText, 1)
            reset()
            ic.setComposingText("", 1)
        }
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

