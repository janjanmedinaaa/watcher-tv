package com.medina.juanantonio.watcher.features.splash

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medina.juanantonio.watcher.di.ApplicationScope
import com.medina.juanantonio.watcher.features.loader.LoaderUseCase
import com.medina.juanantonio.watcher.shared.utils.Event
import com.medina.juanantonio.watcher.sources.auth.AuthUseCase
import com.medina.juanantonio.watcher.sources.auth.IAuthRepository
import com.medina.juanantonio.watcher.sources.content.IContentRepository
import com.medina.juanantonio.watcher.sources.content.WatchHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val contentRepository: IContentRepository,
    private val authRepository: IAuthRepository,
    private val loaderUseCase: LoaderUseCase,
    private val watchHistoryUseCase: WatchHistoryUseCase,
    private val authUseCase: AuthUseCase
) : ViewModel() {

    private val _navigateToHomeScreen = MutableLiveData<Event<Unit>>()
    val navigateToHomeScreen: LiveData<Event<Unit>>
        get() = _navigateToHomeScreen

    val splashState = MutableLiveData<Event<SplashState>>()

    val phoneNumber = MutableLiveData<String>()
    val otpCode = MutableLiveData<String>()
    val isPhoneNumberValid = MediatorLiveData<Boolean>().apply {
        addSource(phoneNumber) {
            value = !it.isNullOrBlank() && it.length == 10
        }
    }

    // Bug fix: Keyboard opens and closes quickly before navigating to the Home Screen
    var preventKeyboardPopup = false

    private var job: Job? = null

    var hasPendingSearchResultToWatch = false

    init {
        viewModelScope.launch {
            contentRepository.clearHomePage()
            contentRepository.setupNavigationBar()
            checkAuthentication()
        }
    }

    fun requestOTP() {
        if (job?.isActive == true) return
        job = viewModelScope.launch {
            loaderUseCase.show()
            val phoneNumber = phoneNumber.value ?: ""
            val isOTPRequestSuccess = authRepository.getOTPForLogin(phoneNumber)
            if (isOTPRequestSuccess) setSplashState(SplashState.INPUT_CODE)
            loaderUseCase.hide()
        }
    }

    fun login() {
        if (job?.isActive == true) return
        job = viewModelScope.launch {
            loaderUseCase.show()
            val phoneNumber = phoneNumber.value ?: ""
            val otpCode = otpCode.value ?: ""
            val isLoginSuccessful = authUseCase.login(phoneNumber, otpCode)

            if (isLoginSuccessful) {
                navigateToHomeScreen(showLoading = true)
            } else {
                this@SplashViewModel.otpCode.value = ""
                loaderUseCase.hide()
            }
        }
    }

    private fun checkAuthentication() {
        viewModelScope.launch {
            val isUserAuthenticated = authRepository.isUserAuthenticated()
            val shouldContinueWithoutAuth = authRepository.shouldContinueWithoutAuth()
            if (isUserAuthenticated) {
                val isRefreshSuccessful = authUseCase.refreshToken()
                if (isRefreshSuccessful) {
                    navigateToHomeScreen()
                    return@launch
                } else {
                    watchHistoryUseCase.clearLocalOnGoingVideos()
                }
            } else if (shouldContinueWithoutAuth || hasPendingSearchResultToWatch) {
                navigateToHomeScreen()
                return@launch
            }

            delay(1000L)
            setSplashState(SplashState.INPUT_PHONE_NUMBER)
        }
    }

    fun navigateToHomeScreen(showLoading: Boolean = false) {
        applicationScope.launch {
            if (showLoading) {
                loaderUseCase.show()
                authRepository.continueWithoutAuth()
            } else {
                setSplashState(SplashState.LOADING)
            }

            val homePageId = contentRepository.navigationItems.firstOrNull()?.id ?: -1
            contentRepository.setPageId(homePageId)
            contentRepository.setupPage(homePageId) {
                preventKeyboardPopup = true
                _navigateToHomeScreen.postValue(Event(Unit))
                if (showLoading) loaderUseCase.hide()
            }
        }
    }

    suspend fun hasCacheVideos(): Boolean =
        watchHistoryUseCase.getOnGoingVideos().isNotEmpty()

    private fun setSplashState(state: SplashState) {
        splashState.postValue(Event(state))
    }
}

enum class SplashState {
    LOADING,
    INPUT_PHONE_NUMBER,
    INPUT_CODE,
}