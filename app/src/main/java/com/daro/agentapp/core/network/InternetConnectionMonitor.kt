package com.daro.agentapp.core.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn

/**
 * Observes the app's default network using application context only.
 * Each collector owns a listener, removed on cancellation. Share in a ViewModel if needed.
 * Connectivity is advisory: always handle errors from the actual API request as well.
 */
class InternetConnectionMonitor(context: Context) {
    private val appContext = context.applicationContext
    private val connectivityManager = appContext.getSystemService(ConnectivityManager::class.java)

    fun currentState(): InternetConnectionState {
        val network = connectivityManager.activeNetwork ?: return InternetConnectionState.Unavailable
        return connectivityManager.getNetworkCapabilities(network).toConnectionState()
    }

    fun isConnected(): Boolean = currentState() == InternetConnectionState.Available

    fun observe(): Flow<InternetConnectionState> = callbackFlow {
        trySend(currentState())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val callback = object : ConnectivityManager.NetworkCallback() {
                private var defaultNetwork: Network? = null

                override fun onAvailable(network: Network) {
                    defaultNetwork = network
                    // Capabilities arrive separately; do not query them synchronously here.
                    trySend(InternetConnectionState.Unknown)
                }

                override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                    if (network == defaultNetwork) trySend(capabilities.toConnectionState())
                }

                override fun onLost(network: Network) {
                    // Losing the old network after a handoff must not mark the new one offline.
                    if (network == defaultNetwork) {
                        defaultNetwork = null
                        trySend(InternetConnectionState.Unavailable)
                    }
                }
            }
            connectivityManager.registerDefaultNetworkCallback(callback)
            awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
        } else {
            // API 23 has no default-network callback. The dynamic broadcast tracks default changes.
            @Suppress("DEPRECATION")
            val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    trySend(currentState())
                }
            }
            appContext.registerReceiver(receiver, filter)
            // Close the gap between taking the first snapshot and registering the receiver.
            trySend(currentState())
            awaitClose { appContext.unregisterReceiver(receiver) }
        }
    }.distinctUntilChanged().conflate().flowOn(Dispatchers.Default)
}

private fun NetworkCapabilities?.toConnectionState(): InternetConnectionState = when {
    this == null -> InternetConnectionState.Unknown
    hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) -> InternetConnectionState.Available
    else -> InternetConnectionState.Unavailable
}
