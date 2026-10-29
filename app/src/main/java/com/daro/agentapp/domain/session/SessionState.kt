package com.daro.agentapp.domain.session

/** Safe for UI observation: credentials never appear in session state. */
sealed interface SessionState {
    data object Loading : SessionState
    data class SignedOut(val reason: SignOutReason = SignOutReason.None) : SessionState
    data class SignedIn(val userId: Long, val role: String, val expiresAtEpochMillis: Long) : SessionState
}

enum class SignOutReason { None, Logout, Expired, StorageUnavailable }
