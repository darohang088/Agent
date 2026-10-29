package com.daro.agentapp.presentation.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel

@Composable
fun LoginRoute(onBack: () -> Unit, onSignedIn: () -> Unit, viewModel: LoginViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(state.isSignedIn) {
        if (state.isSignedIn) onSignedIn()
    }
    LoginScreen(state, viewModel::updatePhone, viewModel::updatePassword, viewModel::submit, onBack)
}
