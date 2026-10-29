package com.daro.agentapp.components

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.daro.agentapp.R
import com.daro.agentapp.components.common.AppTopBar
import com.daro.agentapp.utils.keyboardAsState


@Preview
@Composable
internal fun BizUiWrapperPreview() {
    AppUiWrapper(title = "Mobile Biz") {}
}

/**
 * BizUiWrapper for easy to use Scaffold
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AppUiWrapper(
    modifier: Modifier = Modifier,
    title: String,
    onBack: (() -> Unit)? = null,
    topBar: (@Composable () -> Unit)? = null,
    ignoreTopBarPadding: Boolean = false,
    leftActions: @Composable RowScope.() -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    isStatusBarTextDark: Boolean = true,
    bottomBar: (@Composable () -> Unit)? = null,
    containerColor: Color = colorResource(id = R.color.white),
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    onKeyboardDismiss: () -> Unit = {},
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val isKeyboardVisible = keyboardAsState()
    LaunchedEffect(Unit) {
        if (context is ComponentActivity) {
            // System bar
            WindowCompat.getInsetsController(context.window, context.window.decorView).apply {
                isAppearanceLightStatusBars = isStatusBarTextDark
            }
        }
    }

    /**
     * Hide keyboard when tap outside
     */
    LaunchedEffect(isKeyboardVisible.value) {
        if (!isKeyboardVisible.value) {
            Log.d("BizUiWrapper", "onKeyboardDismiss: ")
            onKeyboardDismiss.invoke()
        }
    }

    Scaffold(
        modifier = Modifier.then(modifier),
        topBar = {
            if (topBar != null) topBar()
            else {
                AppTopBar(
                    title = title, onBack = onBack, leftActions = leftActions, actions = actions
                )
            }
        },
        bottomBar = {
            bottomBar?.invoke()
        },
        containerColor = containerColor,
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = floatingActionButtonPosition
    ) { paddingValues ->
        Box(modifier = Modifier
            .padding(if (ignoreTopBarPadding) PaddingValues(0.dp) else paddingValues)
            .pointerInput(Unit) {
                /**
                 * Set touch events for tapping outside
                 */
                detectTapGestures(onTap = {
                    if (!isKeyboardVisible.value) return@detectTapGestures
                    keyboardController?.hide()
                })
            }) {
            content()
        }
    }
}