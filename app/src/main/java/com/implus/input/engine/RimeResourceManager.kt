package com.implus.input.engine

import android.content.Context
import java.io.File
import java.io.FileOutputStream

/**
 * 负责将 assets 中的 RIME 配置文件释放到内部存储
 */
object RimeResourceManager {
    
    fun deployIfNeeded(context: Context) {
        val rimeDir = File(context.filesDir, "rime")
        if (!rimeDir.exists()) {
            rimeDir.mkdirs()
        }
        
        // 示例：释放基础配置文件
        copyAssetFolder(context, "rime", rimeDir.absolutePath)
    }

    private fun copyAssetFolder(context: Context, assetPath: String, targetPath: String) {
        val assets = context.assets.list(assetPath) ?: return
        if (assets.isEmpty()) {
            copyAssetFile(context, assetPath, targetPath)
        } else {
            File(targetPath).mkdirs()
            for (asset in assets) {
                copyAssetFolder(context, "$assetPath/$asset", "$targetPath/$asset")
            }
        }
    }

    private fun copyAssetFile(context: Context, assetFile: String, targetFile: String) {
        context.assets.open(assetFile).use { input ->
            FileOutputStream(targetFile).use { output ->
                input.copyTo(output)
            }
        }
    }
}
