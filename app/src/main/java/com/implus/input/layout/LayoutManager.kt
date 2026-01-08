package com.implus.input.layout

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import java.io.InputStreamReader

object LayoutManager {
    private val gson = Gson()
    private const val TAG = "LayoutManager"

    fun loadLayout(context: Context, fileName: String): KeyboardLayout? {
        Log.d(TAG, "Attempting to load layout: $fileName")
        return try {
            val inputStream = context.assets.open("layouts/$fileName")
            val reader = InputStreamReader(inputStream)
            val layout = gson.fromJson(reader, KeyboardLayout::class.java)
            reader.close()
            if (layout == null) {
                Log.e(TAG, "Parsed layout is null for $fileName")
                return null
            }
            Log.d(TAG, "Successfully loaded: ${layout.name}")
            layout.pages.forEach { page ->
                val keyCount = page.rows.sumOf { it.keys.size }
                Log.d(TAG, "Page '${page.id}' has $keyCount keys in ${page.rows.size} rows")
            }
            layout
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load layout $fileName: ${e.message}", e)
            null
        }
    }
}