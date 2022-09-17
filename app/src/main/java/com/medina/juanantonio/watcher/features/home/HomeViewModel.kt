package com.medina.juanantonio.watcher.features.home

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medina.juanantonio.watcher.data.models.Video
import com.medina.juanantonio.watcher.data.models.VideoGroup
import com.medina.juanantonio.watcher.data.models.VideoMedia
import com.medina.juanantonio.watcher.shared.utils.Event
import com.medina.juanantonio.watcher.sources.home.IHomePageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val homePageRepository: IHomePageRepository
) : ViewModel() {

    val contentList = MutableLiveData<Event<List<VideoGroup>>>()
    val videoMedia = MutableLiveData<Event<VideoMedia>>()
    val episodeList = MutableLiveData<Event<VideoGroup>>()
    val onGoingVideosList = MutableLiveData<Event<VideoGroup>>()
    val episodeToAutoPlay = MutableLiveData<Event<Video>>()

    private var displaysEpisodes = false
    var contentLoaded = false

    private var job: Job? = null

    fun setupVideoList(episodeList: VideoGroup?) {
        if (contentLoaded) {
            if (episodeList == null) getOnGoingVideoGroup()
            return
        }
        contentLoaded = true

        viewModelScope.launch {
            if (episodeList != null) {
                displaysEpisodes = true
                contentList.value = Event(listOf(episodeList))

                // 1. Gets a Content id of the Series
                episodeList.videoList.firstOrNull()?.contentId?.let { seriesId ->
                    // 2. Check if there is an on going playing episode
                    homePageRepository.getOnGoingVideo(seriesId)?.let { onGoingVideo ->
                        // 3. If there is, get the specific episode to play
                        episodeList.videoList.firstOrNull {
                            it.episodeNumber == onGoingVideo.episodeNumber
                        }?.let { episodeToPlay ->
                            episodeToAutoPlay.value = Event(episodeToPlay)
                        }
                    }
                }
            } else {
                contentList.value = Event(homePageRepository.getHomePage())
                getOnGoingVideoGroup()
            }
        }
    }

    fun getVideoMedia(video: Video) {
        if (job?.isActive == true) return
        job = viewModelScope.launch {
            val videoMedia = homePageRepository.getVideo(
                id = video.contentId,
                category = video.category ?: -1,
                episodeNumber = video.episodeNumber
            )
            videoMedia?.let {
                homePageRepository.currentlyPlayingVideo = video.apply {
                    // Reset video progress
                    videoProgress = 0L
                }
                this@HomeViewModel.videoMedia.value = Event(it)
            }
        }
    }

    private fun getOnGoingVideoGroup() {
        viewModelScope.launch {
            val onGoingVideos = homePageRepository.getOnGoingVideos()
            val onGoingVideoGroup =
                VideoGroup(
                    "Continue Watching",
                    onGoingVideos.map {
                        it.episodeNumber = 0
                        it
                    }
                )

            onGoingVideosList.value = Event(onGoingVideoGroup)
        }
    }

    fun handleSeries(video: Video) {
        if (displaysEpisodes) getVideoMedia(video)
        else getEpisodeList(video)
    }

    fun addNewContent() {
        contentList.value = Event(homePageRepository.getHomePage())
    }

    private fun getEpisodeList(video: Video) {
        if (job?.isActive == true) return
        job = viewModelScope.launch {
            val videoMedia = homePageRepository.getSeriesEpisodes(video)
            videoMedia?.let {
                this@HomeViewModel.episodeList.value = Event(it)
            }
        }
    }
}