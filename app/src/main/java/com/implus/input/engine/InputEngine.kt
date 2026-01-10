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
                return true // 只要之前有联想词，就拦截退格动作，防止 Service 重复删除
            }
            return false
        }

        // 直接拦截 Service 传来的 effText
        if (effText != null && effText.length == 1 && !effText[0].isWhitespace()) {
            composingText += effText
            updateCandidates(ic)
            return true
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
            val suggestions = dictManager.getSuggestions(composingText)
            
            // 核心拦截逻辑：保留用户输入的原始前缀，仅附加词典的剩余部分
            candidates = suggestions.map { word ->
                if (word.length >= composingText.length) {
                    composingText + word.substring(composingText.length)
                } else {
                    word
                }
            }
            
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

