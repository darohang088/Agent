package com.daro.agentapp.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
internal class RegisterRequestDto(
    val phoneNumber: String,
    val password: String,
    val firstName: String,
    val lastName: String,
    val displayName: String,
    val dateBirth: String,
    val gender: String,
    val latitude: Double,
    val longitude: Double,
    val locationName: String,
    val interested: List<String>,
)
