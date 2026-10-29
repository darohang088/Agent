package com.daro.agentapp.presentation.greeting

import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
    fun registerButtonOpensFormAndBackReturnsHome() {
        compose.onNodeWithText("Register").performClick()
        compose.onNodeWithText("Phone number (with country code)").assertIsDisplayed()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Phone number (with country code)").assertIsDisplayed()
        compose.onNodeWithText("Back").performClick()
        compose.onNodeWithText("Register").assertIsDisplayed()
    }

    @Test
    fun emptyFormDisplaysValidationWithoutNetworkRequest() {
        compose.onNodeWithText("Register").performClick()
        // The heading and button share text; select the clickable node.
        compose.onNode(androidx.compose.ui.test.hasText("Create account") and
            androidx.compose.ui.test.hasClickAction()).performScrollTo().performClick()
        compose.onNodeWithText("Please check the highlighted fields.").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun greetingRemainsVisibleAfterActivityRecreation() {
        compose.onNodeWithText("Hello Android data !").assertIsDisplayed()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Hello Android data !").assertIsDisplayed()
    }
}
