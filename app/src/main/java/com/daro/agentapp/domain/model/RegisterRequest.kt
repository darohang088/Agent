package com.daro.agentapp.domain.model

/** dateBirth uses yyyy-MM-dd. Gender is sent exactly as supplied by the API contract. */
class RegisterRequest(
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
) {
    override fun toString(): String = "RegisterRequest([REDACTED])"
}
