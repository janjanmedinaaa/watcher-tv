package com.medina.juanantonio.watcher

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.medina.juanantonio.watcher.MainActivity.Companion.SHOW_MOVIE_BACKGROUND
import com.medina.juanantonio.watcher.shared.utils.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor() : ViewModel() {

    val requestPermissions = MutableLiveData<Event<Unit>>()
    val startDownload = MutableLiveData<Event<Boolean>>()

    private val _onKeyDown = MutableLiveData<Event<Int>>()
    val onKeyDown: LiveData<Event<Int>>
        get() = _onKeyDown

    private val _backgroundImageUrl = MutableLiveData<String?>()
    val backgroundImageUrl: LiveData<String?>
        get() = _backgroundImageUrl

    private val _searchResultToWatch = MutableLiveData<Event<Int>>()
    val searchResultToWatch: LiveData<Event<Int>>
        get() = _searchResultToWatch

    private var currentBackgroundUrl = ""

    fun setBackgroundImage(url: String) {
        _backgroundImageUrl.value = url
        currentBackgroundUrl = url
    }

    fun cancelBackgroundImage() {
        _backgroundImageUrl.value = null
    }

    fun setDefaultBackgroundImage() {
        _backgroundImageUrl.value = ""
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

    fun readySearchResultToWatch(id: Int) {
        _searchResultToWatch.value = Event(id)
    }
}