package com.daro.agentapp.presentation.register

import com.daro.agentapp.core.common.AppError

/** Kept in ViewModel memory only; never persist this form in saved instance state. */
data class RegisterForm(
    val phoneNumber: String = "",
    val password: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val displayName: String = "",
    val dateBirth: String = "",
    val gender: String = "MALE",
    val latitude: String = "",
    val longitude: String = "",
    val locationName: String = "",
    val interested: String = "",
) {
    override fun toString() = "RegisterForm([REDACTED])"
}

enum class RegisterField {
    PhoneNumber, Password, FirstName, LastName, DisplayName, DateBirth,
    Gender, Latitude, Longitude, LocationName, Interested,
}

data class RegisterUiState(
    val form: RegisterForm = RegisterForm(),
    val invalidFields: Set<RegisterField> = emptySet(),
    val isSubmitting: Boolean = false,
    val error: AppError? = null,
    val isRegistered: Boolean = false,
)
