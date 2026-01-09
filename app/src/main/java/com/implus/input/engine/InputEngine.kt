package com.implus.input.engine

import android.view.inputmethod.InputConnection
import com.implus.input.layout.KeyboardKey

interface InputEngine {
    fun processKey(key: KeyboardKey, ic: InputConnection, activeStates: Map<String, Boolean>): Boolean
    fun getCandidates(): List<String>
    fun clear()
}

class RawEngine : InputEngine {
    override fun processKey(key: KeyboardKey, ic: InputConnection, activeStates: Map<String, Boolean>): Boolean {
        // RawEngine 不拦截按键，交给 Service 处理标准输出
        return false
    }

    override fun getCandidates(): List<String> = emptyList()
    override fun clear() {}
}
