package com.daro.agentapp.core.network

/** Available means Android validated internet access, not that a particular API is reachable. */
enum class InternetConnectionState {
    Available,
    Unavailable,
    Unknown,
}
