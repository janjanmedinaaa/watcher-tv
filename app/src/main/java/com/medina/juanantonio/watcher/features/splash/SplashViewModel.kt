package com.medina.juanantonio.watcher.features.splash

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medina.juanantonio.watcher.shared.utils.Event
import com.medina.juanantonio.watcher.sources.content.IContentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val contentRepository: IContentRepository
) : ViewModel() {

    val navigateToHomeScreen = MutableLiveData<Event<Unit>>()

    init {
        viewModelScope.launch {
            contentRepository.clearHomePage()
            contentRepository.setupHomePage(startingPage = 0)
            navigateToHomeScreen.value = Event(Unit)
        }
    }
}