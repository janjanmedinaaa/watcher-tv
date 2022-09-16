package com.medina.juanantonio.watcher.features.player

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medina.juanantonio.watcher.data.models.Video
import com.medina.juanantonio.watcher.shared.utils.Event
import com.medina.juanantonio.watcher.sources.home.IHomePageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val homePageRepository: IHomePageRepository
) : ViewModel(), PlaybackStateMachine {

    private var video: Video? = null

    val savedProgress = MutableLiveData<Event<Long>>()

    private val playbackStateListeners = arrayListOf<PlaybackStateListener>()

    /**
     * Adds a [PlaybackStateListener] to be notified of [VideoPlaybackState] changes.
     */
    fun addPlaybackStateListener(listener: PlaybackStateListener) {
        playbackStateListeners.add(listener)
    }

    /**
     * Removes the [PlaybackStateListener] so it receives no further [VideoPlaybackState] changes.
     */
    fun removePlaybackStateListener(listener: PlaybackStateListener) {
        playbackStateListeners.remove(listener)
    }

    override fun onStateChange(state: VideoPlaybackState) {
        playbackStateListeners.forEach {
            it.onChanged(state)
        }
    }

    override fun onCleared() {
        playbackStateListeners.forEach { it.onDestroy() }
    }

    fun saveVideo(progress: Long) {
        viewModelScope.launch {
            video?.let {
                homePageRepository.addOnGoingVideo(
                    it.apply { videoProgress = progress }
                )
            }
        }
    }

    fun handleVideoEnd() {
        viewModelScope.launch {
            video?.let {
                if (it.episodeNumber + 1 > it.episodeCount)
                    homePageRepository.removeOnGoingVideo(it.contentId)
                else
                    homePageRepository.addOnGoingVideo(
                        it.apply {
                            episodeNumber += 1
                            videoProgress = 0L
                        }
                    )
            }
        }
    }

    fun getVideoDetails(id: Int) {
        viewModelScope.launch {
            val onGoingVideo = homePageRepository.getOnGoingVideo(id)
            val currentlyPlayingVideo = homePageRepository.currentlyPlayingVideo

            // Check if the user played a different episode in an on going series
            val isSameEpisode =
                onGoingVideo?.episodeNumber == currentlyPlayingVideo?.episodeNumber

            video = if (isSameEpisode) onGoingVideo else currentlyPlayingVideo
            savedProgress.value = Event(video?.videoProgress ?: 0L)
        }
    }

    fun cleanUpPlayer() {
        homePageRepository.currentlyPlayingVideo = null
    }
}