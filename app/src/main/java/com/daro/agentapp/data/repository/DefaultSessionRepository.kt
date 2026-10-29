package com.daro.agentapp.data.repository

import com.daro.agentapp.core.common.AppError
import com.daro.agentapp.core.common.AppResult
import com.daro.agentapp.data.local.session.SessionStore
import com.daro.agentapp.data.local.session.StoredSession
import com.daro.agentapp.domain.model.AuthSession
import com.daro.agentapp.domain.session.SessionDataCleaner
import com.daro.agentapp.domain.session.SessionRepository
import com.daro.agentapp.domain.session.SessionState
import com.daro.agentapp.domain.session.SignOutReason
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Application-owned session lifecycle; no credentials are exposed in the observable state. */
internal class DefaultSessionRepository(
    private val store: SessionStore,
    private val scope: CoroutineScope,
    private val cleaners: List<SessionDataCleaner> = emptyList(),
    private val now: () -> Long = System::currentTimeMillis,
) : SessionRepository {
    private val mutableState = MutableStateFlow<SessionState>(SessionState.Loading)
    override val state = mutableState.asStateFlow()
    private val mutex = Mutex()
    private var initialized = false
    private var credentials: StoredSession? = null
    private var expiryJob: Job? = null

    override suspend fun restore() {
        mutex.withLock {
            if (initialized) return
            initialized = true
            try {
                val stored = store.read()
                when {
                    stored == null -> mutableState.value = SessionState.SignedOut()
                    !valid(stored) -> clearLocked(SignOutReason.StorageUnavailable)
                    stored.expiresAtEpochMillis <= now() -> clearLocked(SignOutReason.Expired)
                    else -> publish(stored)
                }
            } catch (e: CancellationException) {
                initialized = false
                throw e
            } catch (_: Exception) {
                clearLocked(SignOutReason.StorageUnavailable)
            }
        }
    }

    override suspend fun establish(session: AuthSession): AppResult<Unit> {
        restore()
        return mutex.withLock {
            if (session.expiresIn <= 0 || session.accessToken.isBlank() || session.refreshToken.isBlank() ||
                !session.tokenType.equals("Bearer", true) || session.user.id < 0 || session.user.role.isBlank()
            ) return@withLock AppResult.Failure(AppError.InvalidResponse)
            val currentTime = now()
            if (currentTime < 0 || session.expiresIn > Long.MAX_VALUE / 1000L) {
                return@withLock AppResult.Failure(AppError.InvalidResponse)
            }
            val duration = session.expiresIn * 1000L
            if (currentTime > Long.MAX_VALUE - duration) return@withLock AppResult.Failure(AppError.InvalidResponse)
            val expiresAt = currentTime + duration
            val stored = StoredSession(session.accessToken, session.refreshToken, session.tokenType,
                expiresAt, session.user.id, session.user.role)
            if (credentials != null && credentials?.userId != stored.userId) {
                val cleanup = clearLocked(SignOutReason.Logout)
                if (cleanup is AppResult.Failure) return@withLock cleanup
            }
            try {
                store.write(stored)
                initialized = true
                publish(stored)
                AppResult.Success(Unit)
            } catch (e: CancellationException) {
                clearLocked(SignOutReason.StorageUnavailable)
                throw e
            } catch (_: Exception) {
                clearLocked(SignOutReason.StorageUnavailable)
                AppResult.Failure(AppError.StorageUnavailable)
            }
        }
    }

    override suspend fun logout(): AppResult<Unit> = mutex.withLock {
        initialized = true
        clearLocked(SignOutReason.Logout)
    }

    override suspend fun accessToken(): String? = mutex.withLock {
        val stored = credentials ?: return@withLock null
        if (stored.expiresAtEpochMillis <= now()) {
            clearLocked(SignOutReason.Expired)
            null
        } else stored.accessToken
    }

    private fun valid(value: StoredSession) = value.accessToken.isNotBlank() &&
        value.refreshToken.isNotBlank() && value.tokenType.equals("Bearer", true) &&
        value.userId >= 0 && value.role.isNotBlank()

    private fun publish(stored: StoredSession) {
        expiryJob?.cancel()
        credentials = stored
        mutableState.value = SessionState.SignedIn(stored.userId, stored.role, stored.expiresAtEpochMillis)
        expiryJob = scope.launch {
            while (true) {
                val remaining = stored.expiresAtEpochMillis - now()
                if (remaining > 0) delay(remaining) else break
            }
            mutex.withLock {
                if (credentials === stored) {
                    expiryJob = null
                    clearLocked(SignOutReason.Expired)
                }
            }
        }
    }

    private suspend fun clearLocked(reason: SignOutReason): AppResult<Unit> = withContext(NonCancellable) {
        expiryJob?.cancel()
        expiryJob = null
        credentials = null
        mutableState.value = SessionState.SignedOut(reason)
        var failed = false
        try { store.clear() } catch (_: Exception) { failed = true }
        cleaners.forEach { cleaner ->
            try { cleaner.clearUserData() } catch (_: Exception) { failed = true }
        }
        if (failed) {
            mutableState.value = SessionState.SignedOut(SignOutReason.StorageUnavailable)
            AppResult.Failure(AppError.StorageUnavailable)
        } else AppResult.Success(Unit)
    }
}
