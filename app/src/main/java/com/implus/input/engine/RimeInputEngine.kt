package com.implus.input.engine

/**
 * RIME 引擎的 JNI 桥接
 */
import android.content.Context
import com.osfans.trime.Rime
import java.io.File

class RimeInputEngine(private val context: Context) : InputEngine {
    
    private var isNativeLoaded = false

    init {
        RimeResourceManager.deployIfNeeded(context)
        try {
            System.loadLibrary("rime_jni")
            val rimeDir = File(context.filesDir, "rime").absolutePath
            // RIME 需要两个目录：共享目录和用户目录，这里暂设为同一个
            isNativeLoaded = Rime.startup(rimeDir, rimeDir)
            if (isNativeLoaded) {
                Rime.deploy()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            isNativeLoaded = false
        }
    }

    override fun processKey(keyCode: Int, label: String?): Boolean {
        if (!isNativeLoaded) return false
        
        // 调用 RIME 处理按键
        // keyCode 转换：如果是字母，直接传 char 值
        val rimeKey = if (label?.length == 1) label[0].toInt() else keyCode
        
        if (Rime.onKey(rimeKey, 0)) {
            // 如果 RIME 消费了按键，检查是否有直接上屏的内容
            return true
        }
        return false
    }

    override fun getCandidates(): List<String> {
        if (!isNativeLoaded) return emptyList()
        return Rime.getCandidates()?.toList() ?: emptyList()
    }

    override fun selectCandidate(index: Int): String? {
        if (!isNativeLoaded) return null
        
        // 获取上屏文本前先选择
        if (Rime.onSelect(index)) {
            val commit = Rime.getCommit()
            reset()
            return commit
        }
        return null
    }

    override fun reset() {
        // RIME 的状态管理通常在内部，这里可以根据需要调用 escape 等
        if (isNativeLoaded) {
            Rime.onKey(0xff1b, 0) // 发送 ESC 键重置状态
        }
    }
}
