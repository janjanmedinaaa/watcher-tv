package com.medina.juanantonio.watcher

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor() : ViewModel() {

    val backgroundImageUrl = MutableLiveData<String?>()
    private var currentBackgroundUrl = ""

    fun setBackgroundImage(url: String) {
        backgroundImageUrl.value = url
        currentBackgroundUrl = url
    }

    fun cancelBackgroundImage() {
        backgroundImageUrl.value = null
    }

    fun setDefaultBackgroundImage() {
        backgroundImageUrl.value = ""
    }

    fun resetBackgroundImage() {
        if (currentBackgroundUrl.isNotEmpty())
            backgroundImageUrl.value = currentBackgroundUrl
    }
}