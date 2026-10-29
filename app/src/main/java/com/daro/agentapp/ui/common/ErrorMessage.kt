package com.daro.agentapp.ui.common

import androidx.annotation.StringRes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.daro.agentapp.R
import com.daro.agentapp.core.common.AppError

@StringRes
fun AppError.messageResource(): Int = when (this) {
    AppError.Configuration -> R.string.error_configuration
    AppError.StorageUnavailable -> R.string.error_storage
    AppError.Connection, AppError.Timeout -> R.string.error_connection_unknown_outcome
    AppError.SecureConnection -> R.string.register_secure_error
    AppError.InvalidInput -> R.string.check_fields
    AppError.InvalidResponse -> R.string.error_invalid_response
    AppError.Rejected -> R.string.error_rejected
    is AppError.Http -> when (statusCode) {
        400, 422 -> R.string.error_rejected
        401, 403 -> R.string.error_unauthorized
        409 -> R.string.error_conflict
        429 -> R.string.register_rate_limit
        else -> R.string.error_server
    }
}

@Composable
fun ErrorMessage(error: AppError, modifier: Modifier = Modifier) {
    Text(stringResource(error.messageResource()), color = MaterialTheme.colorScheme.error, modifier = modifier)
}
