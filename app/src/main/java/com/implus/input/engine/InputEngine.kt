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
    @Suppress("EmptyFunctionBlock")
    override fun reset() {}
}

class DictionaryEngine(private val dictManager: DictionaryManager) : InputEngine {
    private var composingText = ""
    private var candidates = listOf<String>()

    override fun processKey(key: KeyboardKey, effText: String?, effKeyCode: Int, ic: InputConnection, metaState: Int): Boolean {
        var handled = true
        when {
            key.action == "commit" -> reset()
            key.action == "backspace" -> handled = handleBackspace(ic)
            shouldProcessAsComposing(key, effText) -> {
                composingText += effText
                updateCandidates(ic)
            }
            else -> {
                commitComposing(ic)
                handled = false
            }
        }
        return handled
    }

    private fun shouldProcessAsComposing(key: KeyboardKey, effText: String?): Boolean {
        return key.action == null && 
               effText?.length == 1 && 
               !effText[0].isWhitespace()
    }

    private fun handleBackspace(ic: InputConnection): Boolean {
        return if (composingText.isNotEmpty()) {
            composingText = composingText.substring(0, composingText.length - 1)
            updateCandidates(ic)
            true
        } else {
            reset()
            false
        }
    }

    private fun commitComposing(ic: InputConnection?) {
        if (ic == null || composingText.isEmpty()) return
        val cursorPosition = 1
        ic.commitText(composingText, cursorPosition)
        ic.setComposingText("", cursorPosition)
        reset()
    }

    private fun updateCandidates(ic: InputConnection?) {
        if (ic == null) {
            candidates = emptyList()
            return
        }
        
        val cursorPosition = 1
        if (composingText.isNotEmpty()) {
            try {
                ic.setComposingText(composingText, cursorPosition)
                val suggestions = dictManager.getSuggestions(composingText)
                candidates = mapSuggestions(suggestions)
                
                if (candidates.none { it.equals(composingText, ignoreCase = true) }) {
                    candidates = listOf(composingText) + candidates
                }
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                android.util.Log.e("DictionaryEngine", "Candidate update error", e)
                candidates = listOf(composingText)
            }
        } else {
            candidates = emptyList()
            ic.setComposingText("", cursorPosition)
        }
    }

    private fun mapSuggestions(suggestions: List<String>): List<String> {
        return suggestions // 目前直接返回词典词汇，保持其原始大小写
    }

    override fun getCandidates(): List<String> = candidates

    override fun reset() {
        composingText = ""
        candidates = emptyList()
    }
}

