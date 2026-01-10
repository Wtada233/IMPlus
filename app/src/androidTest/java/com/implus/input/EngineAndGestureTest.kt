package com.implus.input

import android.view.KeyEvent
import android.view.MotionEvent
import android.view.inputmethod.InputConnection
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.implus.input.engine.DictionaryEngine
import com.implus.input.layout.KeyboardKey
import com.implus.input.manager.DictionaryManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class EngineAndGestureTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var dictManager: DictionaryManager
    private lateinit var engine: DictionaryEngine

    @Before
    fun setup() {
        dictManager = DictionaryManager(context)
        engine = DictionaryEngine(dictManager)
    }

    /**
     * 测试联想引擎的同步逻辑
     */
    @Test
    fun testDictionaryEngineSynchronization() {
        val committedText = StringBuilder()
        val composingText = AtomicReference<String>("")
        val dummyView = android.view.View(context)

        // 手动 Mock InputConnection
        val mockIc = object : android.view.inputmethod.BaseInputConnection(dummyView, true) {
            override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
                committedText.append(text)
                return true
            }
            override fun setComposingText(text: CharSequence?, newCursorPosition: Int): Boolean {
                composingText.set(text?.toString() ?: "")
                return true
            }
        }

        // 1. 输入 'h'
        engine.processKey(KeyboardKey(), "h", KeyEvent.KEYCODE_UNKNOWN, mockIc, 0)
        assertEquals("h", composingText.get())

        // 2. 输入 'e'
        engine.processKey(KeyboardKey(), "e", KeyEvent.KEYCODE_UNKNOWN, mockIc, 0)
        assertEquals("he", composingText.get())

        // 3. 退格
        engine.processKey(KeyboardKey(action = "backspace"), null, KeyEvent.KEYCODE_DEL, mockIc, 0)
        assertEquals("h", composingText.get())

        // 4. 提交
        engine.processKey(KeyboardKey(action = "commit"), null, KeyEvent.KEYCODE_UNKNOWN, mockIc, 0)
        assertTrue("Composing should be empty after commit", engine.getCandidates().isEmpty())
    }

    /**
     * 测试键盘视图的手势识别 (Fling)
     */
    @Test
    fun testKeyboardGestureDetection() {
        val instrumentation = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        instrumentation.runOnMainSync {
            val view = ImplusKeyboardView(context)
            view.swipeThreshold = 50
            
            var detectedDirection: ImplusKeyboardView.Direction? = null
            view.onSwipeListener = { detectedDirection = it }

            // 模拟向左滑动，增加位移和缩短时间以产生足够速度
            val downTime = android.os.SystemClock.uptimeMillis()
            // 注入事件序列
            view.onTouchEvent(MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, 500f, 100f, 0))
            view.onTouchEvent(MotionEvent.obtain(downTime, downTime + 10, MotionEvent.ACTION_MOVE, 250f, 100f, 0))
            view.onTouchEvent(MotionEvent.obtain(downTime, downTime + 20, MotionEvent.ACTION_UP, 50f, 100f, 0))
            
            assertEquals(50, view.swipeThreshold)
            // 验证是否触发了监听器
            assertNotNull("Should detect a swipe direction", detectedDirection)
            assertEquals(ImplusKeyboardView.Direction.LEFT, detectedDirection)
        }
    }
}
