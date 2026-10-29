package com.daro.agentapp.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.daro.agentapp.presentation.home.FeaturePlaceholderScreen
import com.daro.agentapp.presentation.home.HomeMenuItem
import com.daro.agentapp.presentation.home.HomeScreen

private val homeDestinations = mapOf(
    HomeMenuItem.Transfer to Destination.Transfer,
    HomeMenuItem.Notification to Destination.Notification,
    HomeMenuItem.Account to Destination.Account,
    HomeMenuItem.Approval to Destination.Approval,
    HomeMenuItem.Creation to Destination.Creation,
    HomeMenuItem.Loan to Destination.Loan,
    HomeMenuItem.Deposit to Destination.Deposit,
    HomeMenuItem.WebConfirmPin to Destination.WebConfirmPin,
    HomeMenuItem.MoreRole to Destination.MoreRole,
    HomeMenuItem.MoreSecurityCenter to Destination.MoreSecurityCenter,
    HomeMenuItem.MoreDeviceManagement to Destination.MoreDeviceManagement,
    HomeMenuItem.MoreAboutUs to Destination.MoreAboutUs,
    HomeMenuItem.Web to Destination.Web,
)

internal fun NavGraphBuilder.homeGraph(navController: NavController) {
    composable(
        Destination.Home.route,
        enterTransition = { fadeIn(tween(300)) },
        exitTransition = { fadeOut(tween(300)) },
        popEnterTransition = { fadeIn(tween(300)) },
        popExitTransition = { fadeOut(tween(300)) },
    ) {
        HomeScreen(
            onItemClick = { item ->
                navController.navigate(homeDestinations.getValue(item).route) {
                    launchSingleTop = true
                }
            },
            onBack = { navController.popBackStack() },
        )
    }
    homeDestinations.forEach { (item, destination) ->
        composable(destination.route) {
            FeaturePlaceholderScreen(
                titleRes = item.titleRes,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
