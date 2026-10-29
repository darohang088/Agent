package com.daro.agentapp.presentation.greeting

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.ui.unit.dp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.daro.agentapp.R
import com.daro.agentapp.ui.theme.AgentAppTheme

@Composable
fun GreetingScreen(state: GreetingUiState, onRegister: () -> Unit, onHome: () -> Unit, onLogin: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        when (state) {
            GreetingUiState.Loading -> CircularProgressIndicator()
            is GreetingUiState.Content -> Text(stringResource(R.string.greeting, state.name))
        }
        Button(onClick = onHome) { Text(stringResource(R.string.home)) }
        Button(onClick = onRegister) { Text(stringResource(R.string.register)) }
        Button(onClick = onLogin) { Text(stringResource(R.string.login)) }
    }
}

@Preview(showBackground = true)
@Composable
private fun GreetingScreenPreview() {
    AgentAppTheme { GreetingScreen(GreetingUiState.Content("Android"), onRegister = {}, onHome = {}, onLogin = {}) }
}
