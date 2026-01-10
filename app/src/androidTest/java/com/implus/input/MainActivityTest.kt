package com.implus.input

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun testUIElementsDisplayed() {
        // 验证欢迎标题是否显示
        onView(withId(R.id.tv_welcome_title)).check(matches(isDisplayed()))
        
        // 验证启用按钮是否显示
        onView(withId(R.id.btn_enable_ime)).check(matches(isDisplayed()))
        
        // 验证选择按钮是否显示
        onView(withId(R.id.btn_select_ime)).check(matches(isDisplayed()))
        
        // 验证测试输入框是否显示
        onView(withId(R.id.layout_test_input)).check(matches(isDisplayed()))
    }
}
