package com.daro.agentapp.data.repository

import com.daro.agentapp.core.common.AppError
import com.daro.agentapp.core.common.AppResult
import com.daro.agentapp.data.local.session.SessionStore
import com.daro.agentapp.data.local.session.StoredSession
import com.daro.agentapp.domain.model.AuthSession
import com.daro.agentapp.domain.model.AuthUser
import com.daro.agentapp.domain.session.SessionDataCleaner
import com.daro.agentapp.domain.session.SessionState
import com.daro.agentapp.domain.session.SignOutReason
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DefaultSessionRepositoryTest {
    private class Store : SessionStore {
        var value: StoredSession? = null
        var failRead = false
        var failWrite = false
        var failClear = false
        var clears = 0
        override suspend fun read(): StoredSession? {
            if (failRead) throw IOException("unavailable")
            return value
        }
        override suspend fun write(session: StoredSession) {
            if (failWrite) throw IOException("unavailable")
            value = session
        }
        override suspend fun clear() {
            clears++
            if (failClear) throw IOException("unavailable")
            value = null
        }
    }

    private fun response(id: Long = 7, expiresIn: Long = 60) = AuthSession(
        "synthetic-access", "synthetic-refresh", "Bearer", expiresIn,
        AuthUser(id, "+15555550123", "PHONE", "USER"),
    )

    private fun stored(expiresAt: Long = 60_000) = StoredSession(
        "synthetic-access", "synthetic-refresh", "Bearer", expiresAt, 7, "USER",
    )

    @Test
    fun `empty storage restores signed out`() = runTest {
        val repo = DefaultSessionRepository(Store(), backgroundScope) { testScheduler.currentTime }
        assertEquals(SessionState.Loading, repo.state.value)
        repo.restore()
        assertEquals(SessionState.SignedOut(), repo.state.value)
    }

    @Test
    fun `valid stored credentials restore without exposing tokens in state`() = runTest {
        val store = Store().apply { value = stored() }
        val repo = DefaultSessionRepository(store, backgroundScope) { testScheduler.currentTime }
        repo.restore()
        assertEquals(SessionState.SignedIn(7, "USER", 60_000), repo.state.value)
        assertEquals("synthetic-access", repo.accessToken())
        assertFalse(repo.state.value.toString().contains("synthetic-access"))
        assertFalse(store.value.toString().contains("synthetic-refresh"))
    }

    @Test
    fun `expired storage is cleared and never yields an access token`() = runTest {
        val store = Store().apply { value = stored(0) }
        val repo = DefaultSessionRepository(store, backgroundScope) { testScheduler.currentTime }
        repo.restore()
        assertEquals(SessionState.SignedOut(SignOutReason.Expired), repo.state.value)
        assertNull(repo.accessToken())
        assertNull(store.value)
    }

    @Test
    fun `unreadable or invalid storage fails closed`() = runTest {
        val store = Store().apply { failRead = true }
        val repo = DefaultSessionRepository(store, backgroundScope) { testScheduler.currentTime }
        repo.restore()
        assertEquals(SessionState.SignedOut(SignOutReason.StorageUnavailable), repo.state.value)
        assertNull(repo.accessToken())
        assertEquals(1, store.clears)
    }

    @Test
    fun `establish persists before publishing and supports restoration by new repository`() = runTest {
        val store = Store()
        val repo = DefaultSessionRepository(store, backgroundScope) { testScheduler.currentTime }
        assertEquals(AppResult.Success(Unit), repo.establish(response()))
        val restored = DefaultSessionRepository(store, backgroundScope) { testScheduler.currentTime }
        restored.restore()
        assertEquals(SessionState.SignedIn(7, "USER", 60_000), restored.state.value)
    }

    @Test
    fun `failed persistence does not authenticate`() = runTest {
        val store = Store().apply { failWrite = true }
        val repo = DefaultSessionRepository(store, backgroundScope) { testScheduler.currentTime }
        assertEquals(AppResult.Failure(AppError.StorageUnavailable), repo.establish(response()))
        assertNull(repo.accessToken())
        assertTrue(repo.state.value is SessionState.SignedOut)
    }

    @Test
    fun `logout clears credentials and every registered user cache`() = runTest {
        val store = Store()
        var cleaned = 0
        val repo = DefaultSessionRepository(store, backgroundScope,
            listOf(SessionDataCleaner { cleaned++ }, SessionDataCleaner { cleaned++ })) { testScheduler.currentTime }
        repo.establish(response())
        assertEquals(AppResult.Success(Unit), repo.logout())
        assertNull(store.value)
        assertNull(repo.accessToken())
        assertEquals(2, cleaned)
        repo.restore() // A repeated restore must never resurrect a logged-out session.
        assertEquals(SessionState.SignedOut(SignOutReason.Logout), repo.state.value)
    }

    @Test
    fun `cleanup failure still signs out and attempts remaining cleaners`() = runTest {
        val store = Store()
        var cleaned = false
        val repo = DefaultSessionRepository(store, backgroundScope, listOf(
            SessionDataCleaner { throw IOException("unavailable") },
            SessionDataCleaner { cleaned = true },
        )) { testScheduler.currentTime }
        repo.establish(response())
        store.failClear = true
        assertEquals(AppResult.Failure(AppError.StorageUnavailable), repo.logout())
        assertNull(repo.accessToken())
        assertTrue(repo.state.value is SessionState.SignedOut)
        assertTrue(cleaned)
    }

    @Test
    fun `timer expires session and clears stored credentials`() = runTest {
        val store = Store()
        val repo = DefaultSessionRepository(store, backgroundScope) { testScheduler.currentTime }
        repo.establish(response(expiresIn = 2))
        runCurrent()
        advanceTimeBy(2_001)
        runCurrent()
        assertEquals(SessionState.SignedOut(SignOutReason.Expired), repo.state.value)
        assertNull(store.value)
    }

    @Test
    fun `replacing account clears user data before new session is published`() = runTest {
        val store = Store()
        var cleaned = 0
        val repo = DefaultSessionRepository(store, backgroundScope,
            listOf(SessionDataCleaner { cleaned++ })) { testScheduler.currentTime }
        repo.establish(response(7))
        repo.establish(response(8))
        assertEquals(1, cleaned)
        assertEquals(8L, (repo.state.value as SessionState.SignedIn).userId)
    }

    @Test
    fun `canceling establishment removes credentials written before cancellation`() = runTest {
        var stored: StoredSession? = null
        val written = CompletableDeferred<Unit>()
        val source = object : SessionStore {
            override suspend fun read() = stored
            override suspend fun write(session: StoredSession) {
                stored = session
                written.complete(Unit)
                awaitCancellation()
            }
            override suspend fun clear() { stored = null }
        }
        val repo = DefaultSessionRepository(source, backgroundScope) { testScheduler.currentTime }
        val operation = launch { repo.establish(response()) }
        runCurrent()
        assertTrue(written.isCompleted)
        operation.cancelAndJoin()
        assertNull(stored)
        assertNull(repo.accessToken())
        assertTrue(repo.state.value is SessionState.SignedOut)
    }

    @Test
    fun `logout racing with startup restoration cannot resurrect a session`() = runTest {
        var stored: StoredSession? = stored()
        val readStarted = CompletableDeferred<Unit>()
        val continueRead = CompletableDeferred<Unit>()
        val source = object : SessionStore {
            override suspend fun read(): StoredSession? {
                readStarted.complete(Unit)
                continueRead.await()
                return stored
            }
            override suspend fun write(session: StoredSession) { stored = session }
            override suspend fun clear() { stored = null }
        }
        val repo = DefaultSessionRepository(source, backgroundScope) { testScheduler.currentTime }
        val restoring = launch { repo.restore() }
        runCurrent()
        assertTrue(readStarted.isCompleted)
        val signingOut = launch { repo.logout() }
        continueRead.complete(Unit)
        joinAll(restoring, signingOut)
        assertNull(repo.accessToken())
        assertNull(stored)
        assertEquals(SessionState.SignedOut(SignOutReason.Logout), repo.state.value)
    }

    @Test
    fun `invalid expiry or overflow cannot establish a session`() = runTest {
        val repo = DefaultSessionRepository(Store(), backgroundScope) { testScheduler.currentTime }
        for (expiresIn in listOf(0L, -1L, Long.MAX_VALUE)) {
            assertEquals(AppResult.Failure(AppError.InvalidResponse), repo.establish(response(expiresIn = expiresIn)))
        }
        assertNull(repo.accessToken())
    }
}
