package com.daro.agentapp.presentation.greeting

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun GreetingRoute(factory: ViewModelProvider.Factory, modifier: Modifier = Modifier) {
    val viewModel: GreetingViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    GreetingScreen(state = state, modifier = modifier)
}
