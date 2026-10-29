package com.daro.agentapp.presentation.login

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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.daro.agentapp.R
import com.daro.agentapp.core.common.AppError
import com.daro.agentapp.ui.common.ErrorMessage
import com.daro.agentapp.ui.theme.AgentAppTheme

@Composable
fun LoginScreen(
    state: LoginUiState,
    onPhoneChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
        Text(stringResource(R.string.login), style = MaterialTheme.typography.headlineMedium)
        if (state.isSignedIn) {
            Text(stringResource(R.string.login_success))
        } else {
            OutlinedTextField(
                value = state.phoneNumber, onValueChange = onPhoneChange,
                label = { Text(stringResource(R.string.phone_number)) },
                singleLine = true, enabled = !state.isSubmitting, isError = state.invalidPhone,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.password, onValueChange = onPasswordChange,
                label = { Text(stringResource(R.string.password)) },
                singleLine = true, enabled = !state.isSubmitting, isError = state.invalidPassword,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(),
            )
            if (state.invalidPhone || state.invalidPassword) {
                Text(stringResource(R.string.check_fields), color = MaterialTheme.colorScheme.error)
            }
            state.error?.let { error ->
                if (error == AppError.Rejected || (error is AppError.Http && error.statusCode == 401)) {
                    Text(stringResource(R.string.login_rejected), color = MaterialTheme.colorScheme.error)
                } else ErrorMessage(error)
            }
            if (state.isSubmitting) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Button(onClick = onSubmit, enabled = !state.isSubmitting, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(if (state.isSubmitting) R.string.signing_in else R.string.sign_in))
            }
        }
    }
}

@Preview(showBackground = true, name = "Login form")
@Composable
private fun LoginScreenPreview() {
    AgentAppTheme { LoginScreen(LoginUiState(), {}, {}, {}, {}) }
}
