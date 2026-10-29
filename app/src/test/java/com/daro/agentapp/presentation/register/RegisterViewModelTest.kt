package com.daro.agentapp.presentation.register

import com.daro.agentapp.core.common.AppError
import com.daro.agentapp.core.common.AppResult
import com.daro.agentapp.domain.model.AuthSession
import com.daro.agentapp.domain.model.AuthUser
import com.daro.agentapp.domain.model.RegisterRequest
import com.daro.agentapp.fixtures.FakeAuthRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class RegisterViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val response = CompletableDeferred<AppResult<AuthSession>>()
    private var calls = 0
    private var captured: RegisterRequest? = null
    private val repository = FakeAuthRepository { request ->
        calls++
        captured = request
        response.await()
    }

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `fresh viewmodel does not restore credentials or automatically submit`() = runTest(dispatcher) {
        val original = filledViewModel()
        assertTrue(original.uiState.value.form.password.isNotEmpty())
        val restoredOwner = RegisterViewModel(repository)
        runCurrent()
        assertEquals("", restoredOwner.uiState.value.form.password)
        assertEquals("", restoredOwner.uiState.value.form.phoneNumber)
        assertEquals(0, calls)
    }

    @Test
    fun `empty form does not call endpoint`() = runTest(dispatcher) {
        val vm = RegisterViewModel(repository)
        vm.submit()
        runCurrent()
        assertEquals(0, calls)
        assertTrue(RegisterField.PhoneNumber in vm.uiState.value.invalidFields)
        assertTrue(RegisterField.Password in vm.uiState.value.invalidFields)
        assertFalse(vm.uiState.value.isSubmitting)
    }

    @Test
    fun `invalid date and coordinates do not call endpoint`() = runTest(dispatcher) {
        val vm = filledViewModel()
        vm.updateField(RegisterField.DateBirth, "2026-02-30")
        vm.updateField(RegisterField.Latitude, "NaN")
        vm.updateField(RegisterField.Longitude, "181")
        vm.submit()
        runCurrent()
        assertEquals(0, calls)
        assertTrue(vm.uiState.value.invalidFields.containsAll(setOf(
            RegisterField.DateBirth, RegisterField.Latitude, RegisterField.Longitude,
        )))
    }

    @Test
    fun `submitting twice sends one request and locks editing`() = runTest(dispatcher) {
        val vm = filledViewModel()
        vm.submit()
        vm.submit()
        vm.updateField(RegisterField.FirstName, "Changed")
        assertTrue(vm.uiState.value.isSubmitting)
        runCurrent()
        assertEquals(1, calls)
        assertEquals("Test", captured!!.firstName)
        assertEquals(listOf("music", "books"), captured!!.interested)
        response.complete(AppResult.Failure(AppError.Http(409)))
        runCurrent()
        assertFalse(vm.uiState.value.isSubmitting)
    }

    @Test
    fun `successful registration clears sensitive form and cannot resubmit`() = runTest(dispatcher) {
        val vm = filledViewModel()
        response.complete(AppResult.Success(AuthSession(
            "synthetic-access", "synthetic-refresh", "Bearer", 3600,
            AuthUser(1, "+15555550123", "PHONE", "USER"),
        )))
        vm.submit()
        runCurrent()
        assertTrue(vm.uiState.value.isRegistered)
        assertEquals("", vm.uiState.value.form.password)
        assertFalse(vm.uiState.value.toString().contains("synthetic-access"))
        vm.submit()
        runCurrent()
        assertEquals(1, calls)
    }

    @Test
    fun `failure stays on form without automatic retry`() = runTest(dispatcher) {
        val vm = filledViewModel()
        response.complete(AppResult.Failure(AppError.Timeout))
        vm.submit()
        runCurrent()
        assertEquals(AppError.Timeout, vm.uiState.value.error)
        assertFalse(vm.uiState.value.isRegistered)
        assertFalse(vm.uiState.value.isSubmitting)
        assertEquals("Test", vm.uiState.value.form.firstName)
        assertEquals(1, calls)
        assertFalse(vm.uiState.value.toString().contains("synthetic-password"))
    }

    private fun filledViewModel() = RegisterViewModel(repository).apply {
        updateField(RegisterField.PhoneNumber, "+15555550123")
        updateField(RegisterField.Password, "synthetic-password")
        updateField(RegisterField.FirstName, "Test")
        updateField(RegisterField.LastName, "User")
        updateField(RegisterField.DisplayName, "Test User")
        updateField(RegisterField.DateBirth, "2000-01-01")
        updateField(RegisterField.Latitude, "-90")
        updateField(RegisterField.Longitude, "-180")
        updateField(RegisterField.LocationName, "Test location")
        updateField(RegisterField.Interested, "music, books, ")
    }
}
