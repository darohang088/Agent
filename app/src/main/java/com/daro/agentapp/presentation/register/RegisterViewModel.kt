package com.daro.agentapp.presentation.register

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.viewModelScope
import com.daro.agentapp.core.common.AppResult
import com.daro.agentapp.domain.model.RegisterRequest
import com.daro.agentapp.domain.repository.AuthRepository
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class RegisterViewModel @Inject constructor(private val repository: AuthRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(RegisterUiState())
    val uiState = mutableState.asStateFlow()

    fun updateField(field: RegisterField, value: String) {
        if (uiState.value.isSubmitting || uiState.value.isRegistered) return
        mutableState.update { state ->
            val form = state.form
            val updated = when (field) {
                RegisterField.PhoneNumber -> form.copy(phoneNumber = value)
                RegisterField.Password -> form.copy(password = value)
                RegisterField.FirstName -> form.copy(firstName = value)
                RegisterField.LastName -> form.copy(lastName = value)
                RegisterField.DisplayName -> form.copy(displayName = value)
                RegisterField.DateBirth -> form.copy(dateBirth = value)
                RegisterField.Gender -> form.copy(gender = value)
                RegisterField.Latitude -> form.copy(latitude = value)
                RegisterField.Longitude -> form.copy(longitude = value)
                RegisterField.LocationName -> form.copy(locationName = value)
                RegisterField.Interested -> form.copy(interested = value)
            }
            state.copy(form = updated, invalidFields = state.invalidFields - field, error = null)
        }
    }

    fun submit() {
        val state = uiState.value
        if (state.isSubmitting || state.isRegistered) return
        val form = state.form
        val invalid = validate(form)
        if (invalid.isNotEmpty()) {
            mutableState.update { it.copy(invalidFields = invalid, error = null) }
            return
        }
        mutableState.update { it.copy(isSubmitting = true, error = null, invalidFields = emptySet()) }
        viewModelScope.launch {
            val request = RegisterRequest(
                phoneNumber = form.phoneNumber.trim(), password = form.password,
                firstName = form.firstName.trim(), lastName = form.lastName.trim(),
                displayName = form.displayName.trim(), dateBirth = form.dateBirth.trim(),
                gender = form.gender.trim(), latitude = form.latitude.trim().toDouble(),
                longitude = form.longitude.trim().toDouble(), locationName = form.locationName.trim(),
                interested = form.interested.split(',').map(String::trim).filter(String::isNotEmpty),
            )
            when (val result = repository.register(request)) {
                is AppResult.Success -> mutableState.value = RegisterUiState(isRegistered = true)
                is AppResult.Failure -> mutableState.update {
                    it.copy(isSubmitting = false, error = result.error)
                }
            }
        }
    }

    private fun validate(form: RegisterForm): Set<RegisterField> = buildSet {
        if (!Regex("^\\+?[0-9]{7,15}$").matches(form.phoneNumber.trim())) add(RegisterField.PhoneNumber)
        if (form.password.isBlank()) add(RegisterField.Password)
        if (form.firstName.isBlank()) add(RegisterField.FirstName)
        if (form.lastName.isBlank()) add(RegisterField.LastName)
        if (form.displayName.isBlank()) add(RegisterField.DisplayName)
        val date = form.dateBirth.trim()
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
        val validDate = Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}").matches(date) &&
            runCatching { formatter.parse(date)?.let { formatter.format(it) == date } == true }.getOrDefault(false)
        if (!validDate) add(RegisterField.DateBirth)
        if (form.gender.isBlank()) add(RegisterField.Gender)
        val latitude = form.latitude.trim().toDoubleOrNull()
        val longitude = form.longitude.trim().toDoubleOrNull()
        if (latitude == null || !latitude.isFinite() || latitude !in -90.0..90.0) add(RegisterField.Latitude)
        if (longitude == null || !longitude.isFinite() || longitude !in -180.0..180.0) add(RegisterField.Longitude)
        if (form.locationName.isBlank()) add(RegisterField.LocationName)
    }

}
