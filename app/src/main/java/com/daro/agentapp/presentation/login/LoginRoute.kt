package com.daro.agentapp.presentation.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun LoginRoute(factory: ViewModelProvider.Factory, onBack: () -> Unit, onSignedIn: () -> Unit) {
    val viewModel: LoginViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.isSignedIn) {
        if (state.isSignedIn) onSignedIn()
    }
    LoginScreen(state, viewModel::updatePhone, viewModel::updatePassword, viewModel::submit, onBack)
}
