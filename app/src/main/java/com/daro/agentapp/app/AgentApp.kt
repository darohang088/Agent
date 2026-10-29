package com.daro.agentapp.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.daro.agentapp.presentation.greeting.GreetingRoute
import com.daro.agentapp.ui.theme.AgentAppTheme

@Composable
fun AgentApp(container: AppContainer) {
    AgentAppTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            GreetingRoute(
                factory = container.greetingViewModelFactory,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}
