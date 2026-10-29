package com.daro.agentapp.base

import com.daro.agentapp.core.common.AppError
import com.daro.agentapp.core.common.AppResult
import java.io.IOException
import java.io.InterruptedIOException
import javax.net.ssl.SSLException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import retrofit2.Response

/** Shared transport result mapping only. No scopes, clients, credentials, or repository registry. */
abstract class BaseRepo {
    protected suspend fun <T : Any> apiCall(call: suspend () -> Response<T>): AppResult<T> =
        withContext(Dispatchers.Default) {
            try {
                val response = call()
                if (!response.isSuccessful) {
                    response.errorBody()?.close()
                    AppResult.Failure(AppError.Http(response.code()))
                } else {
                    response.body()?.let { AppResult.Success(it) }
                        ?: AppResult.Failure(AppError.InvalidResponse)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: SSLException) {
                AppResult.Failure(AppError.SecureConnection)
            } catch (_: InterruptedIOException) {
                AppResult.Failure(AppError.Timeout)
            } catch (_: SerializationException) {
                AppResult.Failure(AppError.InvalidResponse)
            } catch (_: IOException) {
                AppResult.Failure(AppError.Connection)
            }
        }
}
