package com.implus.input.layout

import android.content.Context
import com.google.gson.Gson
import java.io.InputStreamReader

object LayoutManager {
    private val gson = Gson()

    /**
     * 从 assets 目录加载并解析布局文件
     */
    fun loadLayout(context: Context, fileName: String): KeyboardLayout? {
        return try {
            val inputStream = context.assets.open("layouts/$fileName")
            val reader = InputStreamReader(inputStream)
            gson.fromJson(reader, KeyboardLayout::class.java).also {
                reader.close()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
