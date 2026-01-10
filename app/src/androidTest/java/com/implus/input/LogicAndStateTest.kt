package com.implus.input

import android.view.KeyEvent
import android.view.inputmethod.InputConnection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.implus.input.engine.DictionaryEngine
import com.implus.input.layout.KeyType
import com.implus.input.layout.KeyboardKey
import com.implus.input.layout.KeyboardLayout
import com.implus.input.layout.KeyboardPage
import com.implus.input.layout.KeyboardRow
import com.implus.input.manager.DictionaryManager
import com.implus.input.manager.KeyboardStateManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class LogicAndStateTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var stateManager: KeyboardStateManager
    private lateinit var dictManager: DictionaryManager
    private lateinit var engine: DictionaryEngine

    @Before
    fun setup() {
        stateManager = KeyboardStateManager()
        dictManager = DictionaryManager(context)
        engine = DictionaryEngine(dictManager)
    }

    @Test
    fun testKeyboardStateManagerMetaCalculation() {
        val layout = KeyboardLayout(
            name = "Test",
            pages = listOf(
                KeyboardPage(rows = listOf(
                    KeyboardRow(keys = listOf(
                        KeyboardKey(id = "shift", type = KeyType.MODIFIER, metaValue = KeyEvent.META_SHIFT_ON, text = com.google.gson.JsonPrimitive("SHIFT_LEFT")),
                        KeyboardKey(id = "ctrl", type = KeyType.MODIFIER, metaValue = KeyEvent.META_CTRL_ON, text = com.google.gson.JsonPrimitive("CTRL_LEFT"))
                    ))
                ))
            )
        )

        stateManager.resetAndPreparse(layout) { 
            if (it?.asString == "SHIFT_LEFT") KeyEvent.KEYCODE_SHIFT_LEFT 
            else if (it?.asString == "CTRL_LEFT") KeyEvent.KEYCODE_CTRL_LEFT
            else KeyEvent.KEYCODE_UNKNOWN
        }

        // Activate Shift
        stateManager.activeStates["shift"] = true
        assertEquals(KeyEvent.META_SHIFT_ON, stateManager.calculateTotalMetaState())

        // Activate Ctrl as well
        stateManager.activeStates["ctrl"] = true
        assertEquals(KeyEvent.META_SHIFT_ON or KeyEvent.META_CTRL_ON, stateManager.calculateTotalMetaState())

        // Deactivate Shift
        stateManager.activeStates["shift"] = false
        assertEquals(KeyEvent.META_CTRL_ON, stateManager.calculateTotalMetaState())
    }

    @Test
    fun testTransientStateReset() {
        // Simulate a transient sticky key (like Shift that turns off after one press)
        stateManager.activeStates["shift"] = true
        stateManager.activeTransientKeyIds.add("shift")

        // First call should reset it and return true
        assertTrue("Should return true when states are reset", stateManager.resetTransientStates())
        assertFalse("Shift state should be false after reset", stateManager.activeStates["shift"] ?: true)
        assertTrue("Transient set should be empty", stateManager.activeTransientKeyIds.isEmpty())

        // Second call should do nothing
        assertFalse("Should return false when nothing to reset", stateManager.resetTransientStates())
    }

    @Test
    fun testDictionaryEngineCandidateLogic() {
        // Mock InputConnection
        val committedText = AtomicReference<String>("")
        val composingText = AtomicReference<String>("")
        
        val mockIc = object : android.view.inputmethod.BaseInputConnection(android.view.View(context), true) {
            override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
                committedText.set(text?.toString() ?: "")
                return true
            }
            override fun setComposingText(text: CharSequence?, newCursorPosition: Int): Boolean {
                composingText.set(text?.toString() ?: "")
                return true
            }
        }

        // 1. Type "t"
        engine.processKey(KeyboardKey(), "t", KeyEvent.KEYCODE_T, mockIc, 0)
        assertEquals("t", composingText.get())
        
        // 2. Type "e" -> "te"
        engine.processKey(KeyboardKey(), "e", KeyEvent.KEYCODE_E, mockIc, 0)
        assertEquals("te", composingText.get())

        // 3. Space should commit "te" (since we didn't load dictionary, it commits raw)
        // Note: The logic for space/punctuation in DictionaryEngine:
        // if key.action == null && !effText[0].isWhitespace() -> composing
        // else -> commitComposing
        
        engine.processKey(KeyboardKey(text=com.google.gson.JsonPrimitive(" ")), " ", KeyEvent.KEYCODE_SPACE, mockIc, 0)
        
        assertEquals("te", committedText.get())
        assertEquals("", composingText.get()) // Composing should be cleared
    }
}