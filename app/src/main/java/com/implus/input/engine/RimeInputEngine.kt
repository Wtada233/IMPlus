package com.implus.input.engine

/**
 * RIME 引擎的 JNI 桥接
 */
import android.content.Context
import android.util.Log
import com.osfans.trime.Rime
import java.io.File

class RimeInputEngine(private val context: Context) : InputEngine {
    
    private var isNativeLoaded = false
    private val TAG = "RimeInputEngine"

    init {
        try {
            Log.d(TAG, "Initializing RIME resources...")
            RimeResourceManager.deployIfNeeded(context)
            
            Log.d(TAG, "Loading rime_jni library...")
            System.loadLibrary("rime_jni")
            
            val rimeDir = File(context.filesDir, "rime").absolutePath
            Log.d(TAG, "Starting RIME with dir: $rimeDir")
            
            // 扩大捕获范围到 Throwable 以处理 Error
            isNativeLoaded = Rime.startup(rimeDir, rimeDir)
            if (isNativeLoaded) {
                Log.d(TAG, "RIME started successfully, deploying...")
                Rime.deploy()
            } else {
                Log.e(TAG, "RIME startup failed (returned false)")
            }
        } catch (t: Throwable) {
            Log.e(TAG, "Fatal error during RIME initialization", t)
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
