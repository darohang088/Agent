package com.daro.agentapp.presentation.greeting

import com.daro.agentapp.data.repository.DefaultGreetingRepository
import com.daro.agentapp.domain.model.Greeting
import com.daro.agentapp.domain.repository.GreetingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GreetingViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() { Dispatchers.setMain(dispatcher) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `starts loading then displays existing greeting`() = runTest(dispatcher) {
        val viewModel = GreetingViewModel(DefaultGreetingRepository())
        assertEquals(GreetingUiState.Loading, viewModel.uiState.value)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        runCurrent()
        assertEquals(GreetingUiState.Content("Android data "), viewModel.uiState.value)
    }

    @Test
    fun `repository changes update screen state`() = runTest(dispatcher) {
        val greetings = MutableStateFlow(Greeting("First"))
        val repository = object : GreetingRepository {
            override fun observeGreeting() = greetings
        }
        val viewModel = GreetingViewModel(repository)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiState.collect()
        }
        runCurrent()
        assertEquals(GreetingUiState.Content("First"), viewModel.uiState.value)
        greetings.value = Greeting("Second")
        runCurrent()
        assertEquals(GreetingUiState.Content("Second"), viewModel.uiState.value)
    }
}
