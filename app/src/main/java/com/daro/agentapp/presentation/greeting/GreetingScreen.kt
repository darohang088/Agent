package com.daro.agentapp.presentation.greeting

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.daro.agentapp.R
import com.daro.agentapp.ui.theme.AgentAppTheme

@Composable
fun GreetingScreen(state: GreetingUiState, modifier: Modifier = Modifier) {
    when (state) {
        GreetingUiState.Loading -> CircularProgressIndicator(modifier = modifier)
        is GreetingUiState.Content -> Text(
            text = stringResource(R.string.greeting, state.name),
            modifier = modifier,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun GreetingScreenPreview() {
    AgentAppTheme { GreetingScreen(GreetingUiState.Content("Android")) }
}
