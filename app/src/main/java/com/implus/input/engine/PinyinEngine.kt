package com.implus.input.engine

import android.content.Context
import android.view.inputmethod.InputConnection
import com.implus.input.layout.KeyboardKey
import com.implus.input.manager.DictionaryManager
import com.pinyin.PinyinIme

/**
 * 基于 AOSP (Google Pinyin) JNI 内核的拼音输入引擎
 */
class PinyinEngine(
    private val context: Context,
    private val dictManager: DictionaryManager
) : InputEngine {

    private val composingBuffer = StringBuilder()
    private var candidates: List<String> = emptyList()
    private var lastIc: InputConnection? = null

    init {
        // 初始化 JNI 解码器
        PinyinIme.init(context)
    }

    override fun processKey(key: KeyboardKey, effText: String?, effKeyCode: Int, ic: InputConnection, metaState: Int): Boolean {
        lastIc = ic
        
        // 处理功能键
        when (key.action) {
            "backspace" -> return handleBackspace(ic)
            "enter" -> {
                if (composingBuffer.isNotEmpty()) {
                    commitText(ic, composingBuffer.toString())
                    return true
                }
            }
            "space" -> {
                if (composingBuffer.isNotEmpty()) {
                    // 空格上屏首选
                    if (candidates.isNotEmpty()) {
                        onCandidateSelected(candidates[0])
                        return true
                    }
                }
            }
        }

        // 处理字母输入 (a-z) 和 ' 分隔符
        if (effText != null && effText.length == 1) {
            val c = effText[0]
            if (c in 'a'..'z' || c in 'A'..'Z' || c == '\'') {
                composingBuffer.append(c.lowercaseChar())
                updateComposing(ic)
                return true
            }
        }

        // 如果有正在输入的内容但按下了非字母键，则上屏并让系统处理该键
        if (composingBuffer.isNotEmpty()) {
            commitText(ic, composingBuffer.toString())
            return false
        }

        return false
    }

    private fun handleBackspace(ic: InputConnection): Boolean {
        if (composingBuffer.isNotEmpty()) {
            composingBuffer.deleteCharAt(composingBuffer.length - 1)
            if (composingBuffer.isEmpty()) {
                resetAndClear(ic)
            } else {
                updateComposing(ic)
            }
            return true
        }
        return false
    }

    private fun updateComposing(ic: InputConnection) {
        val input = composingBuffer.toString()
        
        // 1. 搜索候选词
        val searchResult = PinyinIme.search(input)
        
        // 2. 获取内核处理后的拼写串（用于带分词符显示）
        val spl = PinyinIme.getSpellingString()
        
        // 只有当内核返回了有效拼写串时才使用，否则退回到原始输入
        // AOSP 内核在 zhengzai 下应返回 zheng'zai
        val displayText = if (spl != null && spl.spellingStr.isNotEmpty()) {
            spl.spellingStr
        } else {
            input
        }
        
        ic.setComposingText(displayText, 1)
        
        // 3. 候选词列表：引擎结果 + 原始输入（兜底英文）
        val engineCandidates = searchResult?.toList() ?: emptyList()
        candidates = engineCandidates + listOf(input)
    }

    private fun commitText(ic: InputConnection, text: String) {
        ic.commitText(text, 1)
        reset()
    }

    private fun resetAndClear(ic: InputConnection) {
        ic.setComposingText("", 0)
        reset()
    }

    override fun getCandidates(): List<String> = candidates

    override fun onCandidateSelected(text: String): Boolean {
        val ic = lastIc ?: return false
        val currentInput = composingBuffer.toString()
        android.util.Log.d("PinyinLogic", "onCandidateSelected START: text=$text, currentBuffer=$currentInput")
        
        // 1. 处理原始输入上屏 (英文模式)
        if (text == currentInput) {
            android.util.Log.d("PinyinLogic", "Selecting raw input, full commit")
            commitText(ic, text)
            return false
        }

        // 2. 查找候选词在引擎结果中的索引
        val index = candidates.indexOf(text)
        val isEngineCandidate = index >= 0 && index < candidates.size - 1

        if (isEngineCandidate) {
            android.util.Log.d("PinyinLogic", "Selecting engine candidate at index $index")
            // 告知内核选词
            PinyinIme.choose(index)
            
            // 获取选词后的内核状态
            val spl = PinyinIme.getSpellingString()
            if (spl != null) {
                android.util.Log.d("PinyinLogic", "After choose state: raw=${spl.rawSpelling}, formatted=${spl.spellingStr}, decodedLen=${spl.decodedLen}")
                
                // 提交选中的词
                ic.commitText(text, 1)

                // 计算剩余未消耗的原始拼音
                val remainingRaw = if (spl.decodedLen < spl.rawSpelling.length) {
                    spl.rawSpelling.substring(spl.decodedLen)
                } else {
                    ""
                }

                if (remainingRaw.isEmpty()) {
                    android.util.Log.d("PinyinLogic", "No remaining pinyin, resetting")
                    reset()
                    return false
                } else {
                    // 同步缓冲区并开启新搜索
                    android.util.Log.d("PinyinLogic", "Partial commit: consumed=${spl.decodedLen}, remaining=$remainingRaw")
                    composingBuffer.setLength(0)
                    composingBuffer.append(remainingRaw)
                    
                    // 必须先重新搜索，内核才能更新内部的搜索树
                    // 注意：Google Pinyin 内核在 choose 之后已经自动进入了下一阶段
                    // 我们只需要更新 UI 显示和候选词
                    updateComposing(ic)
                    return true
                }
            } else {
                android.util.Log.e("PinyinLogic", "getSpellingString returned null after choose")
            }
        }
        
        android.util.Log.d("PinyinLogic", "Fallback: commit whole candidate and reset")
        commitText(ic, text)
        return false
    }

    override fun reset() {
        composingBuffer.setLength(0)
        candidates = emptyList()
        PinyinIme.resetSearch()
    }
}
