package com.daro.agentapp.app

import com.daro.agentapp.data.repository.DefaultGreetingRepository
import com.daro.agentapp.domain.repository.GreetingRepository
import com.daro.agentapp.presentation.greeting.GreetingViewModel

class AppContainer {
    private val greetingRepository: GreetingRepository = DefaultGreetingRepository()
    val greetingViewModelFactory = GreetingViewModel.factory(greetingRepository)
}
