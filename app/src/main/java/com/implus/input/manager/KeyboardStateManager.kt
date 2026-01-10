package com.implus.input.manager

import android.view.KeyEvent
import com.implus.input.layout.KeyboardLayout
import com.implus.input.layout.KeyType

/**
 * 专门管理键盘状态（Active States, Meta Maps, Transient Keys）的逻辑类
 */
class KeyboardStateManager {
    val activeStates = mutableMapOf<String, Boolean>()
    val activeTransientKeyIds = mutableSetOf<String>()
    val metaStateMap = mutableMapOf<String, Int>()
    val metaKeyCodeMap = mutableMapOf<String, Int>()

    fun resetAndPreparse(layout: KeyboardLayout, parseKeyCode: (com.google.gson.JsonElement?) -> Int) {
        activeStates.clear()
        activeTransientKeyIds.clear()
        metaStateMap.clear()
        metaKeyCodeMap.clear()
        
        layout.pages.flatMap { it.rows }.flatMap { it.keys }.forEach { k ->
            k.id?.let { id ->
                activeStates[id] = false
                k.metaValue?.let { meta -> metaStateMap[id] = meta }
            }
            k.parsedKeyCode = parseKeyCode(k.text)
            if (k.type == KeyType.MODIFIER && k.id != null && k.parsedKeyCode != KeyEvent.KEYCODE_UNKNOWN) {
                metaKeyCodeMap[k.id] = k.parsedKeyCode
            }
            k.overrides?.values?.forEach { override ->
                override.parsedKeyCode = parseKeyCode(override.text)
            }
        }
    }

    fun calculateTotalMetaState(): Int {
        var meta = 0
        activeStates.forEach { (stateId, isActive) ->
            if (isActive) {
                metaStateMap[stateId]?.let { value ->
                    meta = meta or value
                }
            }
        }
        return meta
    }

    fun resetTransientStates(): Boolean {
        return if (activeTransientKeyIds.isNotEmpty()) {
            activeTransientKeyIds.forEach { id -> activeStates[id] = false }
            activeTransientKeyIds.clear()
            true
        } else false
    }
}
