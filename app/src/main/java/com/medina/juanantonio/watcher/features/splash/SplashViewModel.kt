package com.medina.juanantonio.watcher.features.splash

import android.os.Build
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medina.juanantonio.watcher.BuildConfig
import com.medina.juanantonio.watcher.features.loader.LoaderUseCase
import com.medina.juanantonio.watcher.github.models.ReleaseBean
import com.medina.juanantonio.watcher.github.sources.IUpdateRepository
import com.medina.juanantonio.watcher.shared.utils.Event
import com.medina.juanantonio.watcher.sources.auth.IAuthRepository
import com.medina.juanantonio.watcher.sources.content.IContentRepository
import com.medina.juanantonio.watcher.sources.content.WatchHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val contentRepository: IContentRepository,
    private val updateRepository: IUpdateRepository,
    private val authRepository: IAuthRepository,
    private val loaderUseCase: LoaderUseCase,
    private val watchHistoryUseCase: WatchHistoryUseCase
) : ViewModel() {

    val navigateToHomeScreen = MutableLiveData<Event<Unit>>()
    val newerRelease = MutableLiveData<Event<ReleaseBean>>()
    val splashState = MutableLiveData<Event<SplashState>>()
    var assetToDownload: ReleaseBean.Asset? = null

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

    private val isEmulator: Boolean
        get() = Build.FINGERPRINT.contains("generic")

    init {
        viewModelScope.launch {
            contentRepository.clearHomePage()

            val requestList = arrayListOf<Deferred<Any?>>(
                async { contentRepository.setupNavigationBar() }
            )

            if (updateRepository.shouldGetUpdate()) {
                requestList.add(
                    async { updateRepository.getLatestRelease() }
                )
            }

            val results = requestList.awaitAll()
            val latestRelease = results.firstOrNull { it is ReleaseBean } as? ReleaseBean

            latestRelease?.let { releaseBean ->
                val developerModeEnabled = updateRepository.isDeveloperMode()
                if (shouldDownloadRelease(releaseBean, developerModeEnabled)) {
                    assetToDownload = releaseBean.assets.first { it.isAPK() }
                    newerRelease.value = Event(releaseBean)
                } else checkAuthentication()
            } ?: checkAuthentication()
        }
    }

    fun saveLastUpdateReminder() {
        viewModelScope.launch {
            updateRepository.saveLastUpdateReminder()
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
            val isLoginSuccessful = authRepository.login(phoneNumber, otpCode)

            if (isLoginSuccessful) {
                watchHistoryUseCase.clearLocalCacheVideos()
                navigateToHomeScreen(showLoading = true)
            } else {
                this@SplashViewModel.otpCode.value = ""
                loaderUseCase.hide()
            }
        }
    }

    fun checkAuthentication() {
        viewModelScope.launch {
            val isUserAuthenticated = authRepository.isUserAuthenticated()
            if (isUserAuthenticated) {
                val isRefreshSuccessful = authRepository.refreshToken()
                if (isRefreshSuccessful) {
                    navigateToHomeScreen()
                    return@launch
                } else {
                    authRepository.clearToken()
                }
            }

            delay(1000L)
            setSplashState(SplashState.INPUT_PHONE_NUMBER)
        }
    }

    fun navigateToHomeScreen(showLoading: Boolean = false) {
        viewModelScope.launch {
            if (showLoading) loaderUseCase.show()
            val homePageId = contentRepository.navigationItems.firstOrNull()?.contentId
            contentRepository.setupHomePage(homePageId)

            assetToDownload = null
            preventKeyboardPopup = true
            navigateToHomeScreen.value = Event(Unit)
            if (showLoading) loaderUseCase.hide()
        }
    }

    private fun setSplashState(state: SplashState) {
        splashState.value = Event(state)
    }

    private fun shouldDownloadRelease(
        releaseBean: ReleaseBean,
        developerModeEnabled: Boolean
    ): Boolean {
        return releaseBean.run {
            (developerModeEnabled || isForDownload())
                && isNewerVersion()
                && assets.any { it.isAPK() }
                && (!BuildConfig.DEBUG || isEmulator)
        }
    }
}

enum class SplashState {
    INPUT_PHONE_NUMBER,
    INPUT_CODE,
}