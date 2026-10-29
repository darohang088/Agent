package com.daro.agentapp.domain.model

/** Credentials are transient; never persist this request or log its fields. */
class LoginRequest(val phoneNumber: String, val password: String) {
    override fun toString(): String = "LoginRequest([REDACTED])"
}
