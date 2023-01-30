package com.medina.juanantonio.watcher

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medina.juanantonio.watcher.MainActivity.Companion.SHOW_MOVIE_BACKGROUND
import com.medina.juanantonio.watcher.github.models.ReleaseBean
import com.medina.juanantonio.watcher.github.sources.IUpdateRepository
import com.medina.juanantonio.watcher.shared.utils.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val updateRepository: IUpdateRepository
) : ViewModel() {

    val requestPermissions = MutableLiveData<Event<Unit>>()
    val startDownload = MutableLiveData<Event<Boolean>>()

    private val _onKeyDown = MutableLiveData<Event<Int>>()
    val onKeyDown: LiveData<Event<Int>>
        get() = _onKeyDown

    private val _backgroundImageUrl = MutableLiveData<String?>()
    val backgroundImageUrl: LiveData<String?>
        get() = _backgroundImageUrl

    private val _searchResultToWatch = MutableLiveData<Event<String>>()
    val searchResultToWatch: LiveData<Event<String>>
        get() = _searchResultToWatch

    private val _hasGuestModeCacheVideos = MutableLiveData<Event<Unit>>()
    val hasGuestModeCacheVideos: LiveData<Event<Unit>>
        get() = _hasGuestModeCacheVideos

    private val _hasUpdateRelease = MutableLiveData<Boolean>()
    val hasUpdateRelease: LiveData<Boolean>
        get() = _hasUpdateRelease

    private val _askToUpdate = MutableLiveData<Event<Unit>>()
    val askToUpdate: LiveData<Event<Unit>>
        get() = _askToUpdate

    var updateRelease: ReleaseBean? = null
    val assetToDownload: ReleaseBean.Asset?
        get() = updateRelease?.assets?.lastOrNull { it.isAPK() }

    private var currentBackgroundUrl = ""

    fun setBackgroundImage(url: String) {
        _backgroundImageUrl.value = url
        currentBackgroundUrl = url
    }

    fun cancelBackgroundImage() {
        _backgroundImageUrl.value = null
    }

    fun setMovieBackground() {
        _backgroundImageUrl.value = SHOW_MOVIE_BACKGROUND
    }

    fun resetBackgroundImage() {
        if (currentBackgroundUrl.isNotEmpty())
            _backgroundImageUrl.value = currentBackgroundUrl
    }

    fun requestPermission() {
        requestPermissions.value = Event(Unit)
    }

    fun startDownload(permissionGranted: Boolean) {
        startDownload.value = Event(permissionGranted)
    }

    fun setKeyDown(keyCode: Int) {
        _onKeyDown.value = Event(keyCode)
    }

    fun readySearchResultToWatch(id: String) {
        _searchResultToWatch.value = Event(id)
    }

    fun hasGuestModeCacheVideos() {
        _hasGuestModeCacheVideos.value = Event(Unit)
    }

    fun askToUpdate() {
        _askToUpdate.value = Event(Unit)
    }

    fun saveLastUpdateReminder() {
        viewModelScope.launch {
            updateRepository.saveLastUpdateReminder()
        }
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            if (!updateRepository.shouldGetUpdate()) return@launch
            val latestRelease = updateRepository.getLatestRelease()

            latestRelease?.let { releaseBean ->
                val developerModeEnabled = updateRepository.isDeveloperMode()
                if (shouldDownloadRelease(releaseBean, developerModeEnabled)) {
                    updateRelease = releaseBean
                }
                _hasUpdateRelease.value = assetToDownload != null
            }
        }
    }

    fun clearUpdateRelease() {
        updateRelease = null
        _hasUpdateRelease.value = false
    }

    private fun shouldDownloadRelease(
        releaseBean: ReleaseBean,
        developerModeEnabled: Boolean
    ): Boolean {
        return releaseBean.run {
            (developerModeEnabled || isForDownload())
                && isNewerVersion()
                && assets.any { it.isAPK() }
        }
    }
}