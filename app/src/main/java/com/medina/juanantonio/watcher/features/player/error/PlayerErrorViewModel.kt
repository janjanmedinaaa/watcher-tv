package com.medina.juanantonio.watcher.features.player.error

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medina.juanantonio.watcher.data.models.VideoMedia
import com.medina.juanantonio.watcher.shared.utils.Event
import com.medina.juanantonio.watcher.sources.home.IHomePageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerErrorViewModel @Inject constructor(
    private val homePageRepository: IHomePageRepository
) : ViewModel() {

    val videoMedia = MutableLiveData<Event<VideoMedia>>()
    private var job: Job? = null

    fun getNewVideoMedia() {
        if (job?.isActive == true) return
        job = viewModelScope.launch {
            val video = homePageRepository.currentlyPlayingVideo ?: return@launch
            val videoMedia = homePageRepository.getVideo(
                id = video.contentId,
                category = video.category ?: -1,
                episodeNumber = video.episodeNumber
            )
            videoMedia?.let {
                homePageRepository.currentlyPlayingVideo = video
                this@PlayerErrorViewModel.videoMedia.value = Event(it)
            }
        }
    }
}