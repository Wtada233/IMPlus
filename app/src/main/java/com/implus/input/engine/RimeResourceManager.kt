package com.implus.input.engine

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream

object RimeResourceManager {
    private const val TAG = "RimeResourceManager"
    
    fun deployIfNeeded(context: Context) {
        val rimeDir = File(context.filesDir, "rime")
        if (!rimeDir.exists()) {
            Log.d(TAG, "Creating RIME directory at ${rimeDir.absolutePath}")
            rimeDir.mkdirs()
        }
        
        try {
            // 简单的扁平化释放，后续可根据需要扩展递归逻辑
            val assets = context.assets.list("rime") ?: return
            for (fileName in assets) {
                val outFile = File(rimeDir, fileName)
                // 仅在文件不存在时释放，避免每次启动都覆盖用户配置
                if (!outFile.exists()) {
                    Log.d(TAG, "Deploying asset: $fileName")
                    context.assets.open("rime/$fileName").use { input ->
                        FileOutputStream(outFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to deploy RIME assets", e)
        }
    }
}