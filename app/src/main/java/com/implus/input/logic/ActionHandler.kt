package com.implus.input.logic

import android.view.KeyEvent
import android.view.inputmethod.InputConnection
import com.implus.input.layout.KeyboardLayout
import com.implus.input.layout.KeyboardPage
import com.implus.input.utils.Constants

/**
 * 专门处理键盘 Action（如切换页面、退格等）的逻辑类，减轻 Service 负担
 */
class ActionHandler(
    private val sendKeyFunc: (Int, Int) -> Unit,
    private val hideFunc: () -> Unit,
    private val switchPageFunc: (KeyboardPage) -> Unit,
    private val switchLanguageFunc: () -> Unit
) {
    companion object {
        private const val ACTION_SWITCH_PAGE = "switch_page:"
    }

    fun handleAction(action: String, currentLayout: KeyboardLayout?) {
        when {
            action.startsWith(ACTION_SWITCH_PAGE) -> {
                val pageId = action.substringAfter(ACTION_SWITCH_PAGE)
                currentLayout?.pages?.find { it.id == pageId }?.let { 
                    switchPageFunc(it)
                }
            }
            action == "backspace" -> sendKeyFunc(KeyEvent.KEYCODE_DEL, 0)
            action == "hide" -> hideFunc()
            action == "switch_language" -> switchLanguageFunc()
        }
    }
    
    fun dispatchInput(
        keyCode: Int, 
        text: com.google.gson.JsonElement?, 
        meta: Int, 
        ic: InputConnection,
        sendKeyRaw: (Int, Int) -> Unit
    ) {
        if (keyCode != KeyEvent.KEYCODE_UNKNOWN) {
            sendKeyRaw(keyCode, meta)
        } else {
            text?.let { input ->
                if (input.isJsonPrimitive && input.asJsonPrimitive.isString) {
                    ic.commitText(input.asString, Constants.DEFAULT_CURSOR_OFFSET)
                }
            }
        }
    }
}
