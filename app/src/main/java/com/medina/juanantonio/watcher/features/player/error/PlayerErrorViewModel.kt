package com.medina.juanantonio.watcher.features.player.error

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medina.juanantonio.watcher.data.models.video.VideoMedia
import com.medina.juanantonio.watcher.shared.utils.Event
import com.medina.juanantonio.watcher.sources.media.IMediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerErrorViewModel @Inject constructor(
    private val mediaRepository: IMediaRepository
) : ViewModel() {

    val videoMedia = MutableLiveData<Event<VideoMedia>>()
    private var job: Job? = null

    fun getNewVideoMedia() {
        if (job?.isActive == true) return
        job = viewModelScope.launch {
            val video = mediaRepository.currentlyPlayingVideo ?: return@launch
            val videoMedia = mediaRepository.getVideo(
                id = video.contentId,
                category = video.category ?: -1,
                episodeNumber = video.episodeNumber
            )
            videoMedia?.let {
                mediaRepository.currentlyPlayingVideo = video
                this@PlayerErrorViewModel.videoMedia.value = Event(it)
            }
        }
    }
}