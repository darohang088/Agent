package com.daro.agentapp.presentation.login

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import com.daro.agentapp.core.common.AppResult
import com.daro.agentapp.domain.model.LoginRequest
import com.daro.agentapp.domain.repository.AuthRepository
import com.daro.agentapp.domain.session.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val sessionRepository: SessionRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(LoginUiState())
    val uiState = mutableState.asStateFlow()

    fun updatePhone(value: String) {
        if (uiState.value.isSubmitting || uiState.value.isSignedIn) return
        mutableState.update { it.copy(phoneNumber = value, invalidPhone = false, error = null) }
    }

    fun updatePassword(value: String) {
        if (uiState.value.isSubmitting || uiState.value.isSignedIn) return
        mutableState.update { it.copy(password = value, invalidPassword = false, error = null) }
    }

    fun submit() {
        val state = uiState.value
        if (state.isSubmitting || state.isSignedIn) return
        val invalidPhone = state.phoneNumber.isBlank()
        val invalidPassword = state.password.isEmpty()
        if (invalidPhone || invalidPassword) {
            mutableState.update { it.copy(invalidPhone = invalidPhone, invalidPassword = invalidPassword, error = null) }
            return
        }
        mutableState.update { it.copy(isSubmitting = true, error = null) }
        viewModelScope.launch {
            when (val result = authRepository.login(LoginRequest(state.phoneNumber.trim(), state.password))) {
                is AppResult.Failure -> mutableState.update { it.copy(isSubmitting = false, error = result.error) }
                is AppResult.Success -> when (val saved = sessionRepository.establish(result.value)) {
                    is AppResult.Success -> {
                        mutableState.value = LoginUiState(isSignedIn = true)
                    }
                    is AppResult.Failure -> mutableState.update {
                        it.copy(password = "", isSubmitting = false, error = saved.error)
                    }
                }
            }
        }
    }

}
