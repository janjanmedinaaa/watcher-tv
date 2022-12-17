package com.medina.juanantonio.watcher.sources.auth

import com.medina.juanantonio.watcher.data.manager.IDataStoreManager
import com.medina.juanantonio.watcher.network.Result
import com.medina.juanantonio.watcher.sources.auth.IAuthRepository.Companion.AUTH_TOKEN

class AuthRepository(
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
                dataStoreManager.putString(AUTH_TOKEN, data.token)
                true
            }
        } else false
    }

    override suspend fun refreshToken(): Boolean {
        val result = remoteSource.refreshToken()

        return if (result is Result.Success) {
            val data = result.data?.data
            if (data == null) false
            else {
                dataStoreManager.putString(AUTH_TOKEN, data)
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
}

interface IAuthRepository {
    suspend fun getOTPForLogin(phoneNumber: String): Boolean
    suspend fun login(phoneNumber: String, captcha: String): Boolean
    suspend fun refreshToken(): Boolean
    suspend fun clearToken()
    suspend fun isUserAuthenticated(): Boolean

    companion object {
        const val AUTH_TOKEN = "AUTH_TOKEN"
    }
}