package com.daro.agentapp.app

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.daro.agentapp.navigation.AppNavHost
import com.daro.agentapp.ui.theme.AgentAppTheme

@Composable
fun AgentApp() {
    AgentAppTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            AppNavHost(
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}
