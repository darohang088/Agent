package com.daro.agentapp.presentation.login

import androidx.lifecycle.ViewModelStore
import com.daro.agentapp.core.common.AppError
import com.daro.agentapp.core.common.AppResult
import com.daro.agentapp.domain.model.AuthSession
import com.daro.agentapp.domain.model.AuthUser
import com.daro.agentapp.domain.model.LoginRequest
import com.daro.agentapp.domain.session.SessionRepository
import com.daro.agentapp.domain.session.SessionState
import com.daro.agentapp.fixtures.FakeAuthRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val response = CompletableDeferred<AppResult<AuthSession>>()
    private var calls = 0
    private var captured: LoginRequest? = null
    private var callCanceled = false
    private val auth = FakeAuthRepository(loginHandler = { request ->
        calls++
        captured = request
        try { response.await() } finally { if (!response.isCompleted) callCanceled = true }
    }, handler = { error("Registration was not expected") })
    private class Sessions : SessionRepository {
        override val state = MutableStateFlow<SessionState>(SessionState.SignedOut())
        var saves = 0
        var result: AppResult<Unit> = AppResult.Success(Unit)
        var pending: CompletableDeferred<AppResult<Unit>>? = null
        override suspend fun restore() = Unit
        override suspend fun establish(session: AuthSession): AppResult<Unit> {
            saves++
            return pending?.await() ?: result
        }
        override suspend fun logout() = AppResult.Success(Unit)
        override suspend fun accessToken(): String? = null
    }
    private val sessions = Sessions()
    private fun success() = AppResult.Success(AuthSession("synthetic-access", "synthetic-refresh", "Bearer",
        3600, AuthUser(7, "+15555550123", "PHONE", "USER")))
    private fun filled() = LoginViewModel(auth, sessions).apply {
        updatePhone(" +15555550123 ")
        updatePassword(" synthetic-password ")
    }

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun `empty form highlights fields and makes no call`() = runTest(dispatcher) {
        val vm = LoginViewModel(auth, sessions)
        vm.submit()
        runCurrent()
        assertTrue(vm.uiState.value.invalidPhone)
        assertTrue(vm.uiState.value.invalidPassword)
        assertEquals(0, calls)
    }

    @Test fun `duplicate submission is blocked and password is sent unchanged`() = runTest(dispatcher) {
        val vm = filled()
        vm.submit()
        vm.submit()
        vm.updatePassword("changed")
        runCurrent()
        assertEquals(1, calls)
        assertEquals("+15555550123", captured!!.phoneNumber)
        assertEquals(" synthetic-password ", captured!!.password)
        response.complete(AppResult.Failure(AppError.Http(401)))
        runCurrent()
        assertFalse(vm.uiState.value.isSubmitting)
        assertEquals(0, sessions.saves)
    }

    @Test fun `success is published only after session persistence and credentials are cleared`() = runTest(dispatcher) {
        val vm = filled()
        sessions.pending = CompletableDeferred()
        response.complete(success())
        vm.submit()
        runCurrent()
        assertEquals(1, sessions.saves)
        assertTrue(vm.uiState.value.isSubmitting)
        assertFalse(vm.uiState.value.isSignedIn)
        sessions.pending!!.complete(AppResult.Success(Unit))
        runCurrent()
        assertTrue(vm.uiState.value.isSignedIn)
        assertEquals("", vm.uiState.value.phoneNumber)
        assertEquals("", vm.uiState.value.password)
        assertFalse(vm.uiState.value.toString().contains("synthetic-access"))
        vm.submit()
        runCurrent()
        assertEquals(1, calls)
    }

    @Test fun `session storage failure keeps user on login and clears password`() = runTest(dispatcher) {
        val vm = filled()
        sessions.result = AppResult.Failure(AppError.StorageUnavailable)
        response.complete(success())
        vm.submit()
        runCurrent()
        assertFalse(vm.uiState.value.isSignedIn)
        assertFalse(vm.uiState.value.isSubmitting)
        assertEquals("", vm.uiState.value.password)
        assertEquals(AppError.StorageUnavailable, vm.uiState.value.error)
    }

    @Test fun `network failure does not create a session or automatically retry`() = runTest(dispatcher) {
        val vm = filled()
        response.complete(AppResult.Failure(AppError.Timeout))
        vm.submit()
        runCurrent()
        assertEquals(AppError.Timeout, vm.uiState.value.error)
        assertFalse(vm.uiState.value.isSignedIn)
        assertEquals(0, sessions.saves)
        assertEquals(1, calls)
        assertFalse(vm.uiState.value.toString().contains("synthetic-password"))
    }

    @Test fun `fresh viewmodel has no saved credentials and does not automatically login`() = runTest(dispatcher) {
        filled()
        val restored = LoginViewModel(auth, sessions)
        runCurrent()
        assertEquals("", restored.uiState.value.password)
        assertEquals("", restored.uiState.value.phoneNumber)
        assertEquals(0, calls)
    }

    @Test fun `clearing screen owner cancels in flight login`() = runTest(dispatcher) {
        val vm = filled()
        val owner = ViewModelStore().apply { put("login", vm) }
        vm.submit()
        runCurrent()
        owner.clear()
        runCurrent()
        assertTrue(callCanceled)
        assertEquals(0, sessions.saves)
    }
}
