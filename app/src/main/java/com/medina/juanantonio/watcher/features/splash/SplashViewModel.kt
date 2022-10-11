package com.medina.juanantonio.watcher.features.splash

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medina.juanantonio.watcher.BuildConfig
import com.medina.juanantonio.watcher.github.models.ReleaseBean
import com.medina.juanantonio.watcher.github.sources.IGithubRepository
import com.medina.juanantonio.watcher.shared.utils.Event
import com.medina.juanantonio.watcher.sources.content.IContentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val contentRepository: IContentRepository,
    private val githubRepository: IGithubRepository
) : ViewModel() {

    val navigateToHomeScreen = MutableLiveData<Event<Unit>>()
    val newerRelease = MutableLiveData<Event<ReleaseBean>>()
    var assetToDownload: ReleaseBean.Assets? = null

    init {
        viewModelScope.launch {
            contentRepository.clearHomePage()

            val requestList = listOf(
                async { githubRepository.getLatestRelease() },
                async { contentRepository.setupHomePage(startingPage = 0) }
            )

            val results = requestList.awaitAll()
            val latestRelease = results.firstOrNull { it is ReleaseBean } as? ReleaseBean

            latestRelease?.run {
                if (isForDownload()
                    && isNewerVersion()
                    && assets.any { it.isAPK() }
                    && !BuildConfig.DEBUG
                ) {
                    assetToDownload = assets.first { it.isAPK() }
                    newerRelease.value = Event(this)
                } else navigateToHomeScreen()
            } ?: navigateToHomeScreen()
        }
    }

    fun navigateToHomeScreen() {
        assetToDownload = null
        navigateToHomeScreen.value = Event(Unit)
    }
}