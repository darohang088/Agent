package com.daro.agentapp.ui.common

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.daro.agentapp.core.common.AppError
import com.daro.agentapp.ui.state.LoadState
import com.daro.agentapp.ui.theme.AgentAppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class LoadStateContentTest {
    @get:Rule val compose = createComposeRule()

    @Test fun loadingRendersIndeterminateProgress() {
        compose.setContent { AgentAppTheme { LoadStateContent<String>(LoadState.Loading) { Text(it) } } }
        compose.onNode(SemanticsMatcher.expectValue(
            SemanticsProperties.ProgressBarRangeInfo, ProgressBarRangeInfo.Indeterminate,
        )).assertIsDisplayed()
    }

    @Test fun emptyStateRendersCallerMessageWithoutRetry() {
        compose.setContent {
            AgentAppTheme { LoadStateContent<String>(LoadState.Empty, emptyMessage = "No sample items") { Text(it) } }
        }
        compose.onNodeWithText("No sample items").assertIsDisplayed()
        compose.onNodeWithText("Retry").assertDoesNotExist()
    }

    @Test fun errorRendersSafeMessageAndInvokesExplicitRetry() {
        var retries = 0
        compose.setContent {
            AgentAppTheme {
                LoadStateContent<String>(LoadState.Error(AppError.Configuration), onRetry = { retries++ }) { Text(it) }
            }
        }
        compose.onNodeWithText("This service is not configured for this app version.").assertIsDisplayed()
        compose.onNodeWithText("Retry").performClick()
        assertEquals(1, retries)
    }

    @Test fun contentRendersWithoutErrorOrRetry() {
        compose.setContent { AgentAppTheme { LoadStateContent(LoadState.Content("Sample content")) { Text(it) } } }
        compose.onNodeWithText("Sample content").assertIsDisplayed()
    }
}
