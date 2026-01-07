package com.implus.input.engine

/**
 * RIME 引擎的 JNI 桥接
 */
class RimeInputEngine(private val context: Context) : InputEngine {
    
    private var compositionString = ""
    private var currentCandidates = mutableListOf<String>()

    init {
        RimeResourceManager.deployIfNeeded(context)
        // TODO: 调用 Native 初始化：RimeNative.init(context.filesDir.absolutePath + "/rime")
    }

    override fun processKey(keyCode: Int, label: String?): Boolean {
        // 如果是 a-z，则进入 RIME 处理
        if (label?.length == 1 && label[0] in 'a'..'z') {
            compositionString += label
            updateMockCandidates() // 实际应调用 RimeNative.processKey
            return true
        }
        
        // 如果是空格且有候选词，选择第一个
        if (keyCode == 32 && compositionString.isNotEmpty()) {
            return false // 让 Service 处理 commit 逻辑，或者在此处 selectCandidate(0)
        }

        return false
    }

    private fun updateMockCandidates() {
        // 模拟拼音匹配
        currentCandidates = when (compositionString) {
            "ni" -> mutableListOf("你", "泥", "拟")
            "hao" -> mutableListOf("好", "号", "毫")
            "nihao" -> mutableListOf("你好", "你好吗")
            else -> mutableListOf(compositionString)
        }
    }

    override fun getCandidates(): List<String> = currentCandidates

    override fun selectCandidate(index: Int): String? {
        if (index < currentCandidates.size) {
            val selected = currentCandidates[index]
            reset()
            return selected
        }
        return null
    }

    override fun reset() {
        compositionString = ""
        currentCandidates.clear()
    }
}
