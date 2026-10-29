package com.daro.agentapp.domain.repository

import com.daro.agentapp.domain.model.Greeting
import kotlinx.coroutines.flow.Flow

interface GreetingRepository {
    /** The built-in greeting is immediately available and does not fail. */
    fun observeGreeting(): Flow<Greeting>
}
