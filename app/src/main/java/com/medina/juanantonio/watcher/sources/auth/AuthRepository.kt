package com.medina.juanantonio.watcher.sources.auth

import android.content.Context
import com.medina.juanantonio.watcher.R
import com.medina.juanantonio.watcher.data.manager.IDataStoreManager
import com.medina.juanantonio.watcher.network.Result
import com.medina.juanantonio.watcher.shared.extensions.toastIfNotBlank
import com.medina.juanantonio.watcher.sources.auth.IAuthRepository.Companion.AUTH_TOKEN
import com.medina.juanantonio.watcher.sources.auth.IAuthRepository.Companion.CONTINUE_WITHOUT_AUTH

class AuthRepository(
    private val context: Context,
    private val remoteSource: IAuthRemoteSource,
    private val dataStoreManager: IDataStoreManager
) : IAuthRepository {

    override suspend fun getOTPForLogin(phoneNumber: String, countryCode: String): Boolean {
        val result = remoteSource.getOTPForLogin(phoneNumber, countryCode)

        return result is Result.Success
    }

    override suspend fun login(
        phoneNumber: String,
        captcha: String,
        countryCode: String
    ): String? {
        val result = remoteSource.login(
            phoneNumber = phoneNumber,
            countryCode = countryCode,
            captcha = captcha,
        )

        return if (result is Result.Success) {
            val data = result.data?.data
            data?.token?.let { saveToken(it) }
            data?.token
        } else {
            result.message.toastIfNotBlank(context)
            null
        }
    }

    override suspend fun logout(): Boolean {
        val registrationToken = context.getString(R.string.registration_token)
        val result = remoteSource.logout(registrationToken)
        val isSuccessful = result is Result.Success

        if (isSuccessful) {
            clearToken()
            continueWithoutAuth(false)
        } else {
            result.message.toastIfNotBlank(context)
        }

        return isSuccessful
    }

    override suspend fun refreshToken(): Boolean {
        val result = remoteSource.refreshToken()
        val data = result.data?.data

        return if (result is Result.Success) {
            if (data != null) {
                saveToken(data)
                true
            } else {
                clearToken()
                result.message.toastIfNotBlank(context)
                false
            }
        } else false
    }

    private suspend fun clearToken() {
        dataStoreManager.putString(AUTH_TOKEN, "")
    }

    override suspend fun getUserToken(): String {
        return dataStoreManager.getString(AUTH_TOKEN)
    }

    override suspend fun isUserAuthenticated(): Boolean {
        return dataStoreManager.getString(AUTH_TOKEN).isNotBlank()
    }

    override suspend fun saveToken(token: String) {
        dataStoreManager.putString(AUTH_TOKEN, token)
    }

    override suspend fun continueWithoutAuth(value: Boolean) {
        dataStoreManager.putBoolean(CONTINUE_WITHOUT_AUTH, value)
    }

    override suspend fun shouldContinueWithoutAuth(): Boolean {
        return dataStoreManager.getBoolean(CONTINUE_WITHOUT_AUTH)
    }
}

interface IAuthRepository {
    suspend fun getOTPForLogin(phoneNumber: String, countryCode: String): Boolean

    suspend fun login(phoneNumber: String, captcha: String, countryCode: String): String?
    suspend fun logout(): Boolean
    suspend fun refreshToken(): Boolean
    suspend fun getUserToken(): String
    suspend fun isUserAuthenticated(): Boolean
    suspend fun saveToken(token: String)
    suspend fun continueWithoutAuth(value: Boolean = true)
    suspend fun shouldContinueWithoutAuth(): Boolean

    companion object {
        const val AUTH_TOKEN = "AUTH_TOKEN"
        const val CONTINUE_WITHOUT_AUTH = "CONTINUE_WITHOUT_AUTH"
    }
}