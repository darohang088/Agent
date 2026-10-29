package com.daro.agentapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.daro.agentapp.presentation.greeting.GreetingRoute
import com.daro.agentapp.presentation.login.LoginRoute
import com.daro.agentapp.presentation.register.RegisterRoute

@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    NavHost(navController, startDestination = Destination.Welcome.route, modifier = modifier) {
        composable(Destination.Welcome.route) {
            GreetingRoute(
                onHome = {
                    navController.navigate(Destination.Home.route) { launchSingleTop = true }
                },
                onLogin = {
                    navController.navigate(Destination.Login.route) { launchSingleTop = true }
                },
                onRegister = {
                    navController.navigate(Destination.Register.route) { launchSingleTop = true }
                },
            )
        }
        homeGraph(navController)
        composable(Destination.Login.route) {
            LoginRoute(
                onBack = { navController.popBackStack() },
                onSignedIn = {
                    navController.navigate(Destination.Home.route) {
                        popUpTo(Destination.Login.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(Destination.Register.route) {
            RegisterRoute(
                onBack = { navController.popBackStack() },
            )
        }
    }
}
