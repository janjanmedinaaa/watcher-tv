package com.medina.juanantonio.watcher.features.loader

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoaderUseCase @Inject constructor() {

    private val _loadingStatus = MutableLiveData<Boolean>()
    val loadingStatus: LiveData<Boolean>
        get() = _loadingStatus

    fun show() {
        _loadingStatus.postValue(true)
    }

    fun hide() {
        _loadingStatus.postValue(false)
    }
}