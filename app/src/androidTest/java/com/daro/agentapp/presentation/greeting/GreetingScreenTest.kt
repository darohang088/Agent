package com.daro.agentapp.presentation.greeting

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.daro.agentapp.MainActivity
import org.junit.Rule
import org.junit.Test

class GreetingScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun greetingRemainsVisibleAfterActivityRecreation() {
        compose.onNodeWithText("Hello Android data !").assertIsDisplayed()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Hello Android data !").assertIsDisplayed()
    }
}
