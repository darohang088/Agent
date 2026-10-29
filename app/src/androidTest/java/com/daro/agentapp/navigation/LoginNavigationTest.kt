package com.daro.agentapp.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.pressBack
import com.daro.agentapp.MainActivity
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LoginNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun loginButtonIsBelowRegisterAndOpensLoginForm() {
        val register = compose.onNodeWithText("Register").fetchSemanticsNode().boundsInRoot
        val login = compose.onNodeWithText("Login").fetchSemanticsNode().boundsInRoot
        assertTrue(login.top >= register.bottom)
        compose.onNodeWithText("Login").performClick()
        compose.onNodeWithText("Phone number (with country code)").assertIsDisplayed()
        compose.onNodeWithText("Password").assertIsDisplayed()
        compose.onNodeWithText("Sign in").performClick()
        compose.onNodeWithText("Please check the highlighted fields.").assertIsDisplayed()
        compose.onNodeWithText("Back").performClick()
        compose.onNodeWithText("Register").assertIsDisplayed()
        compose.onNodeWithText("Login").assertIsDisplayed()
    }

    @Test fun loginDestinationSurvivesRecreationAndSystemBackReturnsToWelcome() {
        compose.onNodeWithText("Login").performClick()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Sign in").assertIsDisplayed()
        pressBack()
        compose.onNodeWithText("Register").assertIsDisplayed()
        compose.onNodeWithText("Login").assertIsDisplayed()
    }
}
