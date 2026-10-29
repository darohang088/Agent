package com.daro.agentapp.data.repository

import com.daro.agentapp.domain.model.Greeting
import com.daro.agentapp.domain.repository.GreetingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Owns the existing built-in greeting; no remote or persistent source is required. */
class DefaultGreetingRepository : GreetingRepository {
    override fun observeGreeting(): Flow<Greeting> = flowOf(Greeting(name = "Android data "))
}
