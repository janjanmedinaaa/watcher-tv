package com.medina.juanantonio.watcher.network

import com.medina.juanantonio.watcher.network.models.ApiResponse
import retrofit2.Response

// A generic class that contains data and status about loading this data.
sealed class Result<T>(
    val data: T? = null,
    val message: String? = null,
    val statusCode: Int = 0
) {
    class Loading<T> : Result<T>()
    class Success<T>(data: T?) : Result<T>(data)
    class Error<T>(
        statusCode: Int,
        message: String,
        data: T? = null
    ) : Result<T>(data, message, statusCode)

    class Cancelled<T> : Result<T>()
}

/**
 *  Added extension function of 'Retrofit Response Class' to wrap
 *  network response with our own Result.kt
 *  @see retrofit2.Response
 */
fun <RESPONSE_TYPE> Response<RESPONSE_TYPE>.wrapWithResult(): Result<RESPONSE_TYPE> {
    return if (isSuccessful) Result.Success(body())
    else Result.Error(code(), errorBody()?.string() ?: "")
}

fun <T, RESPONSE_TYPE : ApiResponse<T>> Response<RESPONSE_TYPE>.wrapWithResultForLoklok(): Result<RESPONSE_TYPE> {
    return if (isSuccessful) {
        if (body()?.code == "00000") {
            Result.Success(body())
        } else {
            Result.Error(body()?.code?.toIntOrNull() ?: code(), "${body()?.msg}")
        }
    } else Result.Error(code(), errorBody()?.string() ?: "")
}