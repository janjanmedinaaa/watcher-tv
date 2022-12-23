package com.medina.juanantonio.watcher.sources.auth

import android.content.Context
import com.medina.juanantonio.watcher.R
import com.medina.juanantonio.watcher.data.manager.IDataStoreManager
import com.medina.juanantonio.watcher.network.Result
import com.medina.juanantonio.watcher.sources.auth.IAuthRepository.Companion.AUTH_TOKEN

class AuthRepository(
    private val context: Context,
    private val remoteSource: IAuthRemoteSource,
    private val dataStoreManager: IDataStoreManager
) : IAuthRepository {

    override suspend fun getOTPForLogin(phoneNumber: String): Boolean {
        val result = remoteSource.getOTPForLogin(phoneNumber)

        return result is Result.Success && result.data?.code == "00000"
    }

    override suspend fun login(phoneNumber: String, captcha: String): Boolean {
        val result = remoteSource.login(
            phoneNumber = phoneNumber,
            captcha = captcha
        )

        return if (result is Result.Success) {
            val data = result.data?.data
            if (data?.token == null) false
            else {
                saveToken(data.token)
                true
            }
        } else false
    }

    override suspend fun logout(): Boolean {
        val registrationToken = context.getString(R.string.registration_token)
        val result = remoteSource.logout(registrationToken)
        val isSuccessful =
            result is Result.Success && result.data?.code == "00000"

        if (isSuccessful) clearToken()

        return isSuccessful
    }

    override suspend fun refreshToken(): Boolean {
        val result = remoteSource.refreshToken()

        return if (result is Result.Success) {
            val data = result.data?.data
            if (data == null) false
            else {
                saveToken(data)
                true
            }
        } else false
    }

    override suspend fun clearToken() {
        dataStoreManager.putString(AUTH_TOKEN, "")
    }

    override suspend fun isUserAuthenticated(): Boolean {
        return dataStoreManager.getString(AUTH_TOKEN).isNotBlank()
    }

    private suspend fun saveToken(token: String) {
        dataStoreManager.putString(AUTH_TOKEN, token)
    }
}

interface IAuthRepository {
    suspend fun getOTPForLogin(phoneNumber: String): Boolean
    suspend fun login(phoneNumber: String, captcha: String): Boolean
    suspend fun logout(): Boolean
    suspend fun refreshToken(): Boolean
    suspend fun clearToken()
    suspend fun isUserAuthenticated(): Boolean

    companion object {
        const val AUTH_TOKEN = "AUTH_TOKEN"
    }
}