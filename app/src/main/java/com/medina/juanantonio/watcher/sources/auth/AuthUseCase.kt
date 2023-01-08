package com.medina.juanantonio.watcher.sources.auth

import com.medina.juanantonio.watcher.data.models.user.User
import com.medina.juanantonio.watcher.sources.user.IUserRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthUseCase @Inject constructor(
    private val authRepository: IAuthRepository,
    private val userRepository: IUserRepository,
    private val userDatabase: IUserDatabase
) {

    suspend fun login(phoneNumber: String, captcha: String): Boolean {
        val userToken = authRepository.login(phoneNumber, captcha)

        userToken?.let { token ->
            val userInfo = userRepository.getUserInfo()
            userInfo?.let {
                val user = User(
                    id = it.userId,
                    username = it.nickName,
                    token = token,
                    imageUrl = it.headImgUrl,
                    mobileNo = phoneNumber,
                    countryCode = it.countryCode
                )

                userDatabase.addUser(user)
            }
        }

        return !userToken.isNullOrBlank()
    }

    suspend fun logout(userId: String): Boolean {
        val isLogoutSuccessful = authRepository.logout()

        if (isLogoutSuccessful) {
            val user = userDatabase.getUser(userId)
            userDatabase.deleteUser(user?.id ?: "")
        }

        return isLogoutSuccessful
    }

    suspend fun refreshToken(userId: String? = null): Boolean {
        // Override user token for token HTTP header
        userId?.let { userIdToLogin ->
            val userToken = userDatabase.getUser(userIdToLogin)?.token ?: ""
            authRepository.saveToken(userToken)
        }

        val isRefreshSuccessful = authRepository.refreshToken()

        return if (isRefreshSuccessful) {
            val userInfo = userRepository.getUserInfo()
            val currentUserToken = authRepository.getUserToken()
            userInfo?.let { info ->
                val isUserSaved = userDatabase.getUser(userInfo.userId) != null

                if (isUserSaved) {
                    userDatabase.updateUserInfo(
                        id = info.userId,
                        username = info.userId,
                        imageUrl = info.headImgUrl,
                        token = currentUserToken
                    )
                    true
                } else {
                    logout(userInfo.userId)
                    false
                }
            } ?: false
        } else false
    }
}