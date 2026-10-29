package com.daro.agentapp.presentation.register

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun RegisterRoute(factory: ViewModelProvider.Factory, onBack: () -> Unit) {
    val viewModel: RegisterViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    RegisterScreen(
        state = state,
        onFieldChange = viewModel::updateField,
        onSubmit = viewModel::submit,
        onBack = onBack,
    )
}
