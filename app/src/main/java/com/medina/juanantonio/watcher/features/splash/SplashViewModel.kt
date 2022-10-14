package com.medina.juanantonio.watcher.features.splash

import android.os.Build
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medina.juanantonio.watcher.BuildConfig
import com.medina.juanantonio.watcher.github.models.ReleaseBean
import com.medina.juanantonio.watcher.github.sources.IUpdateRepository
import com.medina.juanantonio.watcher.shared.utils.Event
import com.medina.juanantonio.watcher.sources.content.IContentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val contentRepository: IContentRepository,
    private val updateRepository: IUpdateRepository
) : ViewModel() {

    val navigateToHomeScreen = MutableLiveData<Event<Unit>>()
    val newerRelease = MutableLiveData<Event<ReleaseBean>>()
    var assetToDownload: ReleaseBean.Asset? = null

    private val isEmulator: Boolean
        get() = Build.FINGERPRINT.contains("generic")

    init {
        viewModelScope.launch {
            contentRepository.clearHomePage()

            val requestList = arrayListOf<Deferred<Any?>>(
                async { contentRepository.setupHomePage(startingPage = 0) }
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
                } else navigateToHomeScreen()
            } ?: navigateToHomeScreen()
        }
    }

    fun navigateToHomeScreen() {
        assetToDownload = null
        navigateToHomeScreen.value = Event(Unit)
    }

    fun saveLastUpdateReminder() {
        viewModelScope.launch {
            updateRepository.saveLastUpdateReminder()
            navigateToHomeScreen()
        }
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