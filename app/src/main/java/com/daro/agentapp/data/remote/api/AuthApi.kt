package com.daro.agentapp.data.remote.api

import com.daro.agentapp.data.remote.dto.LoginRequestDto
import com.daro.agentapp.data.remote.dto.RegisterRequestDto
import com.daro.agentapp.data.remote.dto.AuthResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

internal interface AuthApi {
    @POST("api/v1/auth/login")
    suspend fun login(@Body request: LoginRequestDto): Response<AuthResponseDto>

    @POST("api/v1/auth/register")
    suspend fun register(@Body request: RegisterRequestDto): Response<AuthResponseDto>
}
