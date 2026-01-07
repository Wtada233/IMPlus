package com.implus.input.engine

class LatinInputEngine : InputEngine {
    override fun processKey(keyCode: Int, label: String?): Boolean {
        // 英文模式下暂不处理复杂逻辑，直接返回 false 让 Service 处理 commitText
        return false
    }

    override fun getCandidates(): List<String> = emptyList()

    override fun selectCandidate(index: Int): String? = null

    override fun reset() {}
}
