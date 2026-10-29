package com.daro.agentapp.base

import com.daro.agentapp.core.common.AppError
import com.daro.agentapp.core.common.AppResult
import java.io.IOException
import java.net.SocketTimeoutException
import javax.net.ssl.SSLHandshakeException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerializationException
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Response

class BaseRepoTest {
    private class Repository : BaseRepo() {
        suspend fun execute(block: suspend () -> Response<String>) = apiCall(block)
    }

    @Test
    fun `expected failures map without exposing exception messages`() = runBlocking {
        val repository = Repository()
        for ((exception, error) in listOf(
            IOException("private") to AppError.Connection,
            SocketTimeoutException("private") to AppError.Timeout,
            SSLHandshakeException("private") to AppError.SecureConnection,
            SerializationException("private") to AppError.InvalidResponse,
        )) {
            assertEquals(AppResult.Failure(error), repository.execute { throw exception })
        }
    }

    @Test
    fun `cancellation is never mapped into an error`() = runBlocking {
        val cancellation = CancellationException("canceled")
        try {
            Repository().execute { throw cancellation }
            fail("Expected cancellation")
        } catch (actual: CancellationException) {
            assertEquals(cancellation.message, actual.message)
        }
    }

    @Test
    fun `programming errors propagate instead of becoming connection errors`() = runBlocking {
        val exception = IllegalStateException("programming error")
        try {
            Repository().execute { throw exception }
            fail("Expected programming error")
        } catch (actual: IllegalStateException) {
            assertEquals(exception.message, actual.message)
        }
    }
}
