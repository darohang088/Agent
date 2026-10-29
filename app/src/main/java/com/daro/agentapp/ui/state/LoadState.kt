package com.daro.agentapp.ui.state

import com.daro.agentapp.core.common.AppError

/** For content-loading screens. Forms retain their feature-specific input/submission states. */
sealed interface LoadState<out T> {
    data object Loading : LoadState<Nothing>
    data object Empty : LoadState<Nothing>
    data class Content<T>(val value: T) : LoadState<T>
    data class Error(val error: AppError) : LoadState<Nothing>
}
