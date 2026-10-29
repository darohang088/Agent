package com.daro.agentapp.presentation.greeting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewModelScope
import com.daro.agentapp.domain.repository.GreetingRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class GreetingViewModel(repository: GreetingRepository) : ViewModel() {
    val uiState: StateFlow<GreetingUiState> = repository.observeGreeting()
        .map { GreetingUiState.Content(name = it.name) as GreetingUiState }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = GreetingUiState.Loading,
        )

    companion object {
        fun factory(repository: GreetingRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { GreetingViewModel(repository) }
        }
    }
}
