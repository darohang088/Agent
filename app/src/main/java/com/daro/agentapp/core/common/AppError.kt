package com.daro.agentapp.core.common

/** Safe categories only; never retain raw response bodies or exception messages. */
sealed interface AppError {
    data object Configuration : AppError
    data object StorageUnavailable : AppError
    data object Connection : AppError
    data object Timeout : AppError
    data object SecureConnection : AppError
    data object InvalidResponse : AppError
    data object Rejected : AppError
    data object InvalidInput : AppError
    data class Http(val statusCode: Int) : AppError
}
