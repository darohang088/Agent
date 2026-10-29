package com.daro.agentapp.presentation.login

import com.daro.agentapp.core.common.AppError

data class LoginUiState(
    val phoneNumber: String = "",
    val password: String = "",
    val invalidPhone: Boolean = false,
    val invalidPassword: Boolean = false,
    val isSubmitting: Boolean = false,
    val isSignedIn: Boolean = false,
    val error: AppError? = null,
) {
    override fun toString() = "LoginUiState([REDACTED])"
}
