package com.daro.agentapp.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test

class InternetConnectionMonitorTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun snapshotMatchesAndroidValidatedDefaultNetwork() {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val active = manager.activeNetwork
        val capabilities = manager.getNetworkCapabilities(active)
        val expected = when {
            active == null -> InternetConnectionState.Unavailable
            capabilities == null -> InternetConnectionState.Unknown
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) ->
                InternetConnectionState.Available
            else -> InternetConnectionState.Unavailable
        }
        assertEquals(expected, InternetConnectionMonitor(context).currentState())
    }

    @Test
    fun observationEmitsAndCanBeCollectedAgainAfterCancellation() = runBlocking {
        val monitor = InternetConnectionMonitor(context)
        repeat(3) {
            val observed = withTimeout(5_000) {
                monitor.observe().first { it != InternetConnectionState.Unknown }
            }
            assertEquals(monitor.currentState(), observed)
        }
    }
}
