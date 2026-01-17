package com.implus.input

import android.view.KeyEvent
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.implus.input.logic.ActionHandler
import com.implus.input.layout.KeyboardLayout
import com.implus.input.layout.KeyboardPage
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class ActionHandlerTest {

    @Test
    fun testHandleActionBackspace() {
        val lastKeyCode = AtomicInteger(-1)
        val handler = ActionHandler(
            sendKeyFunc = { code, _ -> lastKeyCode.set(code) },
            hideFunc = {},
            switchPageFunc = {},
            switchLanguageFunc = {}
        )

        handler.handleAction("backspace", null)
        assertEquals(KeyEvent.KEYCODE_DEL, lastKeyCode.get())
    }

    @Test
    fun testHandleActionHide() {
        val hideCalled = AtomicBoolean(false)
        val handler = ActionHandler(
            sendKeyFunc = { _, _ -> },
            hideFunc = { hideCalled.set(true) },
            switchPageFunc = {},
            switchLanguageFunc = {}
        )

        handler.handleAction("hide", null)
        assertTrue(hideCalled.get())
    }

    @Test
    fun testHandleActionSwitchPage() {
        val targetPageId = "symbols"
        val switchedPage = AtomicReference<KeyboardPage>(null)
        val layout = KeyboardLayout(
            name = "Test",
            pages = listOf(
                KeyboardPage(id = "main"),
                KeyboardPage(id = targetPageId)
            )
        )
        
        val handler = ActionHandler(
            sendKeyFunc = { _, _ -> },
            hideFunc = {},
            switchPageFunc = { switchedPage.set(it) },
            switchLanguageFunc = {}
        )

        handler.handleAction("switch_page:$targetPageId", layout)
        assertNotNull(switchedPage.get())
        assertEquals(targetPageId, switchedPage.get()?.id)
    }

    @Test
    fun testHandleActionSwitchLanguage() {
        val switchCalled = AtomicBoolean(false)
        val handler = ActionHandler(
            sendKeyFunc = { _, _ -> },
            hideFunc = {},
            switchPageFunc = {},
            switchLanguageFunc = { switchCalled.set(true) }
        )

        handler.handleAction("switch_language", null)
        assertTrue("switch_language action should trigger callback", switchCalled.get())
    }
}
