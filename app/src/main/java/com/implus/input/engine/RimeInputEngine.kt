package com.implus.input.engine

/**
 * RIME 引擎的 JNI 桥接
 */
class RimeInputEngine : InputEngine {
    
    companion object {
        init {
            // System.loadLibrary("rime_jni")
        }
    }

    // 原生方法定义 (暂未实现)
    // external fun initialize(configPath: String, userDataPath: String): Boolean
    // external fun processKey(keyCode: Int, mask: Int): Boolean
    // external fun getCommit(): String?
    // external fun getCandidates(): List<String>

    override fun processKey(keyCode: Int, label: String?): Boolean {
        // TODO: 调用 JNI 处理中文输入
        return false
    }

    override fun getCandidates(): List<String> {
        return emptyList()
    }

    override fun selectCandidate(index: Int): String? {
        return null
    }

    override fun reset() {
        // TODO: 重置 RIME 状态
    }
}
