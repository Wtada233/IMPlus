package com.implus.input.model

import android.content.Context
import com.google.gson.Gson
import java.io.InputStreamReader

object LayoutLoader {
    private val gson = Gson()

    fun loadLayout(context: Context, fileName: String): KeyboardLayout? {
        return try {
            val inputStream = context.assets.open("layouts/$fileName")
            val reader = InputStreamReader(inputStream)
            gson.fromJson(reader, KeyboardLayout::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
