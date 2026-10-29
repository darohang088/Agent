package com.daro.agentapp.presentation.register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.daro.agentapp.ui.theme.AgentAppTheme
import com.daro.agentapp.R
import com.daro.agentapp.ui.common.ErrorMessage

@Composable
fun RegisterScreen(
    state: RegisterUiState,
    onFieldChange: (RegisterField, String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
        Text(stringResource(R.string.create_account), style = MaterialTheme.typography.headlineMedium)
        if (state.isRegistered) {
            Text(stringResource(R.string.register_success))
            Button(onClick = onBack) { Text(stringResource(R.string.return_home)) }
        } else {
            val form = state.form
            RegisterInput(RegisterField.PhoneNumber, form.phoneNumber, R.string.phone_number, state, onFieldChange, KeyboardType.Phone)
            RegisterInput(RegisterField.Password, form.password, R.string.password, state, onFieldChange, KeyboardType.Password)
            RegisterInput(RegisterField.FirstName, form.firstName, R.string.first_name, state, onFieldChange)
            RegisterInput(RegisterField.LastName, form.lastName, R.string.last_name, state, onFieldChange)
            RegisterInput(RegisterField.DisplayName, form.displayName, R.string.display_name, state, onFieldChange)
            RegisterInput(RegisterField.DateBirth, form.dateBirth, R.string.date_birth, state, onFieldChange)
            RegisterInput(RegisterField.Gender, form.gender, R.string.gender, state, onFieldChange)
            RegisterInput(RegisterField.Latitude, form.latitude, R.string.latitude, state, onFieldChange, KeyboardType.Text)
            RegisterInput(RegisterField.Longitude, form.longitude, R.string.longitude, state, onFieldChange, KeyboardType.Text)
            RegisterInput(RegisterField.LocationName, form.locationName, R.string.location_name, state, onFieldChange)
            RegisterInput(RegisterField.Interested, form.interested, R.string.interested, state, onFieldChange)
            if (state.invalidFields.isNotEmpty()) {
                Text(stringResource(R.string.check_fields), color = MaterialTheme.colorScheme.error)
            }
            state.error?.let {
                ErrorMessage(it)
            }
            if (state.isSubmitting) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Button(onClick = onSubmit, enabled = !state.isSubmitting, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(if (state.isSubmitting) R.string.registering else R.string.create_account))
            }
        }
    }
}

@Composable
private fun RegisterInput(
    field: RegisterField,
    value: String,
    label: Int,
    state: RegisterUiState,
    onFieldChange: (RegisterField, String) -> Unit,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value, onValueChange = { onFieldChange(field, it) },
        label = { Text(stringResource(label)) },
        enabled = !state.isSubmitting, isError = field in state.invalidFields,
        singleLine = true, modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (field == RegisterField.Password) PasswordVisualTransformation()
            else VisualTransformation.None,
    )
}


@Preview(showBackground = true, name = "Registration form")
@Composable
private fun RegisterScreenPreview() {
    AgentAppTheme {
        RegisterScreen(RegisterUiState(), onFieldChange = { _, _ -> }, onSubmit = {}, onBack = {})
    }
}
