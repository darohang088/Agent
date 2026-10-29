package com.daro.agentapp.presentation.greeting

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import com.daro.agentapp.domain.repository.GreetingRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class GreetingViewModel @Inject constructor(repository: GreetingRepository) : ViewModel() {
    val uiState: StateFlow<GreetingUiState> = repository.observeGreeting()
        .map { GreetingUiState.Content(name = it.name) as GreetingUiState }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = GreetingUiState.Loading,
        )

}
