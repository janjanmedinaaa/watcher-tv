package com.medina.juanantonio.watcher.sources.auth

import android.content.Context
import android.os.Build
import com.medina.juanantonio.watcher.network.ApiService
import com.medina.juanantonio.watcher.sources.BaseRemoteSource
import com.medina.juanantonio.watcher.network.Result
import com.medina.juanantonio.watcher.network.models.auth.*
import com.medina.juanantonio.watcher.network.wrapWithResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRemoteSource(
    context: Context,
    private val apiService: ApiService
) : BaseRemoteSource(context), IAuthRemoteSource {

    override suspend fun getOTPForLogin(
        phoneNumber: String,
        countryCode: String,
        sendType: String
    ): Result<BasicResponse> {
        return try {
            val request = GetOTPRequest(
                mobile = phoneNumber,
                countryCode = countryCode,
                sendType = sendType
            )

            val response = withContext(Dispatchers.IO) {
                apiService.getOTPForLogin(request)
            }
            response.wrapWithResult()
        } catch (exception: CancellationException) {
            Result.Cancelled()
        } catch (exception: Exception) {
            getDefaultErrorResponse()
        }
    }

    override suspend fun login(
        phoneNumber: String,
        countryCode: String,
        captcha: String,
        phoneModel: String,
        phoneSystem: String,
        adjustId: String
    ): Result<LoginResponse> {
        return try {
            val request = LoginRequest(
                mobile = phoneNumber,
                countryCode = countryCode,
                captcha = captcha,
                phoneModel = phoneModel,
                phoneSystem = phoneSystem,
                adjustId = adjustId
            )

            val response = withContext(Dispatchers.IO) {
                apiService.login(request)
            }
            response.wrapWithResult()
        } catch (exception: CancellationException) {
            Result.Cancelled()
        } catch (exception: Exception) {
            getDefaultErrorResponse()
        }
    }

    override suspend fun logout(registrationToken: String): Result<BasicResponse> {
        return try {
            val request = LogoutRequest(registrationToken)
            val response = withContext(Dispatchers.IO) {
                apiService.logout(request)
            }
            response.wrapWithResult()
        } catch (exception: CancellationException) {
            Result.Cancelled()
        } catch (exception: Exception) {
            getDefaultErrorResponse()
        }
    }

    override suspend fun refreshToken(): Result<RefreshTokenResponse> {
        return try {
            val response = withContext(Dispatchers.IO) {
                apiService.refreshToken()
            }
            response.wrapWithResult()
        } catch (exception: CancellationException) {
            Result.Cancelled()
        } catch (exception: Exception) {
            getDefaultErrorResponse()
        }
    }
}

interface IAuthRemoteSource {
    suspend fun getOTPForLogin(
        phoneNumber: String,
        countryCode: String = "63",
        sendType: String = "2"
    ): Result<BasicResponse>

    suspend fun login(
        phoneNumber: String,
        countryCode: String = "63",
        captcha: String,
        phoneModel: String = Build.MODEL,
        phoneSystem: String = Build.HARDWARE,
        adjustId: String = Build.ID
    ): Result<LoginResponse>

    suspend fun logout(registrationToken: String): Result<BasicResponse>

    suspend fun refreshToken(): Result<RefreshTokenResponse>
}