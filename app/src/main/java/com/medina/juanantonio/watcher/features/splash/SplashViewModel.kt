package com.medina.juanantonio.watcher.features.splash

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medina.juanantonio.watcher.shared.utils.Event
import com.medina.juanantonio.watcher.sources.home.IHomePageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val homePageRepository: IHomePageRepository
) : ViewModel() {

    val navigateToHomeScreen = MutableLiveData<Event<Unit>>()

    init {
        viewModelScope.launch {
            homePageRepository.clearHomePage()
            homePageRepository.setupHomePage(startingPage = 0)
            navigateToHomeScreen.value = Event(Unit)
        }
    }
}