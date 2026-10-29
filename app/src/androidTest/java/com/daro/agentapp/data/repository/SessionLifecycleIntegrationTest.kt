package com.daro.agentapp.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith
import androidx.test.platform.app.InstrumentationRegistry
import com.daro.agentapp.core.common.AppResult
import com.daro.agentapp.data.local.session.KeystoreSessionStore
import com.daro.agentapp.domain.model.AuthSession
import com.daro.agentapp.domain.model.AuthUser
import com.daro.agentapp.domain.session.SessionDataCleaner
import com.daro.agentapp.domain.session.SessionState
import com.daro.agentapp.domain.session.SignOutReason
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test

/** Real Android Keystore/storage; synthetic credentials never reach the backend or logs. */
@RunWith(AndroidJUnit4::class)
class SessionLifecycleIntegrationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private fun session(seconds: Long = 60) = AuthSession(
        "synthetic-access", "synthetic-refresh", "Bearer", seconds,
        AuthUser(7, "+15555550123", "PHONE", "USER"),
    )

    @Test fun establishedSessionRestoresWithNewRepositoryAndStore() = runBlocking {
        val id = "lifecycle-${UUID.randomUUID()}"
        val store = KeystoreSessionStore(context, id)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val restoredScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val first = DefaultSessionRepository(store, scope)
            assertEquals(AppResult.Success(Unit), first.establish(session()))
            scope.cancel() // Drop the original repository's lifetime before restoring from disk.
            val restored = DefaultSessionRepository(KeystoreSessionStore(context, id), restoredScope)
            restored.restore()
            assertTrue(restored.state.value is SessionState.SignedIn)
            assertEquals("synthetic-access", restored.accessToken())
            val ciphertext = File(context.noBackupFilesDir, "$id/session.enc").readBytes()
            assertFalse(ciphertext.toString(Charsets.UTF_8).contains("synthetic-access"))
        } finally {
            scope.cancel(); restoredScope.cancel(); store.clear()
        }
    }

    @Test fun expiredPersistedSessionIsClearedOnRestore() = runBlocking {
        val id = "lifecycle-${UUID.randomUUID()}"
        val store = KeystoreSessionStore(context, id)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val first = DefaultSessionRepository(store, scope, now = { 1_000L })
            assertEquals(AppResult.Success(Unit), first.establish(session()))
            val restored = DefaultSessionRepository(KeystoreSessionStore(context, id), scope, now = { 61_001L })
            restored.restore()
            assertEquals(SessionState.SignedOut(SignOutReason.Expired), restored.state.value)
            assertNull(restored.accessToken())
            assertNull(store.read())
        } finally { scope.cancel(); store.clear() }
    }

    @Test fun timerExpiryRemovesRealStoredCredentials() = runBlocking {
        val id = "lifecycle-${UUID.randomUUID()}"
        val store = KeystoreSessionStore(context, id)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        try {
            val repo = DefaultSessionRepository(store, scope)
            assertEquals(AppResult.Success(Unit), repo.establish(session(seconds = 1)))
            withTimeout(10_000) { repo.state.first { it == SessionState.SignedOut(SignOutReason.Expired) } }
            // Acquiring the repository lock also waits for expiry cleanup to finish.
            assertNull(repo.accessToken())
            assertNull(store.read())
        } finally { scope.cancel(); store.clear() }
    }

    @Test fun logoutClearsRealStorageAndDoesNotRestoreSession() = runBlocking {
        val id = "lifecycle-${UUID.randomUUID()}"
        val store = KeystoreSessionStore(context, id)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        var cleaned = false
        try {
            val repo = DefaultSessionRepository(store, scope, listOf(SessionDataCleaner { cleaned = true }))
            assertEquals(AppResult.Success(Unit), repo.establish(session()))
            assertEquals(AppResult.Success(Unit), repo.logout())
            assertTrue(cleaned)
            assertNull(repo.accessToken())
            assertNull(store.read())
            val restored = DefaultSessionRepository(KeystoreSessionStore(context, id), scope)
            restored.restore()
            assertTrue(restored.state.value is SessionState.SignedOut)
            assertNull(restored.accessToken())
        } finally { scope.cancel(); store.clear() }
    }
}
