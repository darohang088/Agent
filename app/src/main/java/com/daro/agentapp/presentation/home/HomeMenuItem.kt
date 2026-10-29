package com.daro.agentapp.presentation.home

import androidx.annotation.StringRes
import com.daro.agentapp.R

/** Menu identifiers are independent of navigation route strings. */
enum class HomeMenuItem(@param:StringRes val titleRes: Int) {
    Transfer(R.string.menu_transfer),
    Notification(R.string.menu_notification),
    Account(R.string.menu_account),
    Approval(R.string.menu_approval),
    Creation(R.string.menu_creation),
    Loan(R.string.menu_loan),
    Deposit(R.string.menu_deposit),
    WebConfirmPin(R.string.menu_web_confirm_pin),
    MoreRole(R.string.menu_more_roles),
    MoreSecurityCenter(R.string.menu_more_security_center),
    MoreDeviceManagement(R.string.menu_more_device_management),
    MoreAboutUs(R.string.menu_more_about_us),
    Web(R.string.menu_web);
}
