package com.implus.input.engine

/**
 * RIME 引擎的 JNI 桥接
 */
class RimeInputEngine(private val context: Context) : InputEngine {
    
    private var compositionString = ""
    private var currentCandidates = mutableListOf<String>()
    private var isNativeLoaded = false

    init {
        RimeResourceManager.deployIfNeeded(context)
        try {
            System.loadLibrary("rime") // 这里的名称需与 jniLibs 下的文件名一致
            isNativeLoaded = true
            // RimeNative.init(context.filesDir.absolutePath + "/rime")
        } catch (e: UnsatisfiedLinkError) {
            e.printStackTrace()
            // 如果加载失败，将继续使用 Mock 模式，不至于崩溃
        }
    }

    override fun processKey(keyCode: Int, label: String?): Boolean {
        if (!isNativeLoaded) {
            // 如果没有 native 库，走 Mock 逻辑演示效果
            return handleMockLogic(keyCode, label)
        }
        
        // TODO: 调用真正的 RimeNative 接口
        return false
    }

    private fun handleMockLogic(keyCode: Int, label: String?): Boolean {
        // 如果是 a-z，则进入 Mock 处理
        if (label?.length == 1 && label[0] in 'a'..'z') {
            compositionString += label
            updateMockCandidates()
            return true
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
