package com.daro.agentapp.navigation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.espresso.Espresso.pressBack
import com.daro.agentapp.MainActivity
import com.daro.agentapp.presentation.home.HomeMenuItem
import org.junit.Rule
import org.junit.Test

class HomeNavigationTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun everyMenuItemOpensItsDestinationAndReturnsToMenu() {
        compose.onNodeWithText("Home").performClick()
        HomeMenuItem.entries.forEach { item ->
            val title = compose.activity.getString(item.titleRes)
            compose.onNodeWithTag("home_menu").performScrollToNode(hasText(title))
            compose.onNodeWithText(title).performClick()
            compose.onNodeWithText(title).assertIsDisplayed()
            compose.onNodeWithText("This feature is coming soon.").assertIsDisplayed()
            compose.onNodeWithText("Back").performClick()
            compose.onNodeWithTag("home_menu").assertIsDisplayed()
        }
        compose.onNodeWithText("Back").performClick()
        compose.onNodeWithText("Register").assertIsDisplayed()
    }

    @Test
    fun featureDestinationSurvivesRecreationAndSystemBackReturnsToMenu() {
        compose.onNodeWithText("Home").performClick()
        compose.onNodeWithText("Transfer").performClick()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Transfer").assertIsDisplayed()
        compose.onNodeWithText("This feature is coming soon.").assertIsDisplayed()
        pressBack()
        compose.onNodeWithTag("home_menu").assertIsDisplayed()
        pressBack()
        compose.onNodeWithText("Register").assertIsDisplayed()
    }
}
