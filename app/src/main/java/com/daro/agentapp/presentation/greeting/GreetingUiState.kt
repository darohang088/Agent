package com.daro.agentapp.presentation.greeting

sealed interface GreetingUiState {
    data object Loading : GreetingUiState
    data class Content(val name: String) : GreetingUiState
}
