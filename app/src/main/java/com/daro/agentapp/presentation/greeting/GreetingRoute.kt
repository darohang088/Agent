package com.daro.agentapp.presentation.greeting

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun GreetingRoute(onRegister: () -> Unit, onHome: () -> Unit, onLogin: () -> Unit, modifier: Modifier = Modifier, viewModel: GreetingViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    GreetingScreen(state = state, onRegister = onRegister, onHome = onHome, onLogin = onLogin, modifier = modifier)
}
