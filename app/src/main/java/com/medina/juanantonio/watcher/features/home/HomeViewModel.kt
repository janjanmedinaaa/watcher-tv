package com.medina.juanantonio.watcher.features.home

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medina.juanantonio.watcher.data.models.Video
import com.medina.juanantonio.watcher.data.models.VideoGroup
import com.medina.juanantonio.watcher.data.models.VideoMedia
import com.medina.juanantonio.watcher.features.loader.LoaderUseCase
import com.medina.juanantonio.watcher.shared.utils.Event
import com.medina.juanantonio.watcher.sources.content.IContentRepository
import com.medina.juanantonio.watcher.sources.media.IMediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val contentRepository: IContentRepository,
    private val mediaRepository: IMediaRepository,
    private val loaderUseCase: LoaderUseCase
) : ViewModel() {

    val contentList = MutableLiveData<Event<List<VideoGroup>>>()
    val videoMedia = MutableLiveData<Event<VideoMedia>>()
    val selectedVideoGroup = MutableLiveData<Event<VideoGroup>>()
    val onGoingVideosList = MutableLiveData<Event<VideoGroup>>()
    val episodeToAutoPlay = MutableLiveData<Event<Video>>()

    private var isDisplayingEpisodes = false
    var contentLoaded = false

    private var job: Job? = null

    fun setupVideoList(videoGroup: VideoGroup?) {
        if (contentLoaded) {
            if (videoGroup == null) getOnGoingVideoGroup()
            return
        }
        contentLoaded = true

        viewModelScope.launch {
            if (videoGroup != null) {
                val isSeries = videoGroup.videoList.all { !it.isMovie }

                if (isSeries) {
                    isDisplayingEpisodes = true
                    contentList.value = Event(listOf(videoGroup))

                    // 1. Gets a Content id of the Series
                    videoGroup.videoList.firstOrNull()?.contentId?.let { seriesId ->
                        // 2. Check if there is an on going playing episode
                        mediaRepository.getOnGoingVideo(seriesId)?.let { onGoingVideo ->
                            // 3. If there is, get the specific episode to play
                            videoGroup.videoList.firstOrNull {
                                it.episodeNumber == onGoingVideo.episodeNumber
                            }?.let { episodeToPlay ->
                                delay(250)
                                episodeToAutoPlay.value = Event(episodeToPlay)
                            }
                        }
                    }
                } else {
                    contentList.value = Event(listOf(videoGroup))
                }
            } else {
                contentList.value = Event(contentRepository.getHomePage())
                getOnGoingVideoGroup()
            }
        }
    }

    fun getVideoMedia(video: Video) {
        if (job?.isActive == true) return
        job = viewModelScope.launch {
            loaderUseCase.show()
            val videoMedia = mediaRepository.getVideo(
                id = video.contentId,
                category = video.category ?: -1,
                episodeNumber = video.episodeNumber
            )
            videoMedia?.let {
                mediaRepository.currentlyPlayingVideo = video.apply {
                    // Reset video progress
                    videoProgress = 0L
                    score = videoMedia.score
                }
                this@HomeViewModel.videoMedia.value = Event(it)
            }
            loaderUseCase.hide()
        }
    }

    private fun getOnGoingVideoGroup() {
        viewModelScope.launch {
            val onGoingVideos = contentRepository.getOnGoingVideos()
            val latestOnGoingVideos = onGoingVideos.map {
                it.episodeNumber = 0
                it
            }.sortedByDescending { it.lastWatchTime }.take(10)

            val onGoingVideoGroup =
                VideoGroup(
                    category = "Continue Watching",
                    videoList = latestOnGoingVideos,
                    contentType = VideoGroup.ContentType.VIDEOS
                )

            onGoingVideosList.value = Event(onGoingVideoGroup)
        }
    }

    fun getAlbumDetails(video: Video) {
        if (job?.isActive == true) return
        job = viewModelScope.launch {
            loaderUseCase.show()
            val albumDetails = contentRepository.getAlbumDetails(video.contentId)
            albumDetails?.let {
                this@HomeViewModel.selectedVideoGroup.value = Event(it)
            }
            loaderUseCase.hide()
        }
    }

    fun handleSeries(video: Video) {
        if (isDisplayingEpisodes) getVideoMedia(video)
        else getEpisodeList(video)
    }

    fun addNewContent() {
        contentList.value = Event(contentRepository.getHomePage())
    }

    private fun getEpisodeList(video: Video) {
        if (job?.isActive == true) return
        job = viewModelScope.launch {
            loaderUseCase.show()
            val videoMedia = mediaRepository.getSeriesEpisodes(video)
            videoMedia?.let {
                this@HomeViewModel.selectedVideoGroup.value = Event(it)
            }
            loaderUseCase.hide()
        }
    }
}