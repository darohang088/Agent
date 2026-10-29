package com.daro.agentapp.navigation

/** Destinations used by the app navigation graph. */
internal sealed class Destination(val route: String) {
    data object Welcome : Destination("welcome")
    data object Home : Destination("home")
    data object Login : Destination("login")
    data object Register : Destination("register")
    data object Transfer : Destination("transfer")
    data object Notification : Destination("notification")
    data object Account : Destination("account")
    data object Approval : Destination("approval")
    data object Creation : Destination("creation")
    data object Loan : Destination("loan")
    data object Deposit : Destination("deposit")
    data object WebConfirmPin : Destination("web_confirm_pin")
    data object MoreRole : Destination("more_roles")
    data object MoreSecurityCenter : Destination("more_security_center")
    data object MoreDeviceManagement : Destination("more_device_management")
    data object MoreAboutUs : Destination("more_about_us")
    data object Web : Destination("web")
}
