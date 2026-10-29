package com.daro.agentapp.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
internal class LoginRequestDto(val phoneNumber: String, val password: String)
