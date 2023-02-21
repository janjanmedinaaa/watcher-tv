package com.medina.juanantonio.watcher.features.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsModalViewModel @Inject constructor(): ViewModel() {
    private val _settingsModalButton = MutableLiveData<SettingsModalButton>()
    val settingsModalButton: LiveData<SettingsModalButton>
        get() = _settingsModalButton

    val currentModalButton: SettingsModalButton
        get() = _settingsModalButton.value ?: SettingsModalButton.EXIT

    fun setModalButton(button: SettingsModalButton) {
        _settingsModalButton.value = button
    }
}

enum class SettingsModalButton {
    EXIT,
    BACK
}