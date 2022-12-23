package com.medina.juanantonio.watcher.features.home

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medina.juanantonio.watcher.data.models.Video
import com.medina.juanantonio.watcher.data.models.VideoGroup
import com.medina.juanantonio.watcher.data.models.VideoMedia
import com.medina.juanantonio.watcher.features.loader.LoaderUseCase
import com.medina.juanantonio.watcher.network.models.auth.GetUserInfoResponse
import com.medina.juanantonio.watcher.network.models.home.NavigationItemBean
import com.medina.juanantonio.watcher.network.models.player.GetVideoDetailsResponse
import com.medina.juanantonio.watcher.shared.Constants.VideoGroupTitle.ContinueWatchingTitle
import com.medina.juanantonio.watcher.shared.utils.Event
import com.medina.juanantonio.watcher.sources.content.IContentRepository
import com.medina.juanantonio.watcher.sources.content.WatchHistoryUseCase
import com.medina.juanantonio.watcher.sources.media.IMediaRepository
import com.medina.juanantonio.watcher.sources.user.IUserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val contentRepository: IContentRepository,
    private val mediaRepository: IMediaRepository,
    private val loaderUseCase: LoaderUseCase,
    private val watchHistoryUseCase: WatchHistoryUseCase,
    private val userRepository: IUserRepository
) : ViewModel() {

    val contentList = MutableLiveData<Event<List<VideoGroup>>>()
    val videoMedia = MutableLiveData<Event<VideoMedia>>()
    val selectedVideoGroup = MutableLiveData<Event<VideoGroup>>()
    val onGoingVideosList = MutableLiveData<Event<VideoGroup>>()
    val episodeToAutoPlay = MutableLiveData<Event<Video>>()
    val removeNavigationContent = MutableLiveData<Event<Unit>>()
    val videoDetails = MutableLiveData<GetVideoDetailsResponse.Data>()
    val userDetails = MutableLiveData<GetUserInfoResponse.Data>()

    val navigationItems: List<NavigationItemBean>
        get() = contentRepository.navigationItems

    private var isDisplayingEpisodes = false
    var contentLoaded = false

    private var job: Job? = null
    private var videoDetailsJob: Job? = null

    fun setupVideoList(videoGroup: VideoGroup?) {
        if (contentLoaded) {
            if (videoGroup == null) viewModelScope.launch {
                getOnGoingVideoGroup()
            }
            return
        }
        contentLoaded = true

        viewModelScope.launch {
            if (videoGroup != null) {
                val areAlbumItems = videoGroup.videoList.any { it.isAlbumItem }

                if (!areAlbumItems) {
                    isDisplayingEpisodes = true
                    contentList.value = Event(listOf(videoGroup))

                    // 1. Gets a Content id of the Series
                    videoGroup.videoList.firstOrNull()?.contentId?.let { seriesId ->
                        // 2. Check if there is an on going playing episode
                        watchHistoryUseCase.getOnGoingVideo(seriesId)?.let { onGoingVideo ->
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
                getOnGoingVideoGroup()
                contentList.value = Event(contentRepository.getHomePage())
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

    private suspend fun getOnGoingVideoGroup() {
        val onGoingVideos = watchHistoryUseCase.getOnGoingVideos()
        val latestOnGoingVideos = onGoingVideos.map {
            it.episodeNumber = 0
            it.isHomeDisplay = true
            it
        }.sortedByDescending { it.lastWatchTime }.take(10)

        val onGoingVideoGroup =
            VideoGroup(
                category = ContinueWatchingTitle,
                videoList = latestOnGoingVideos,
                contentType = VideoGroup.ContentType.VIDEOS
            )

        onGoingVideosList.value = Event(onGoingVideoGroup)
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

    fun handleNavigationItem(id: Int) {
        if (job?.isActive == true) job?.cancel()
        job = viewModelScope.launch {
            loaderUseCase.show()
            contentRepository.setupHomePage(id)
            contentRepository.resetPage()
            removeNavigationContent()
            addNewContent()
            loaderUseCase.hide()
        }
    }

    private fun removeNavigationContent() {
        removeNavigationContent.value = Event(Unit)
    }

    fun addNewContent() {
        contentList.value = Event(contentRepository.getHomePage())
    }

    fun getVideoDetails(video: Video) {
        if (video.isAlbum) getVideoDetailsFromAlbum(video)
        else getVideoDetailsFromContent(video)
    }

    private fun getVideoDetailsFromContent(video: Video) {
        if (videoDetailsJob?.isActive == true)
            videoDetailsJob?.cancel()

        videoDetailsJob = viewModelScope.launch {
            val videoDetails = mediaRepository.getVideoDetails(video) ?: return@launch
            this@HomeViewModel.videoDetails.value = videoDetails
        }
    }

    private fun getVideoDetailsFromAlbum(video: Video) {
        if (videoDetailsJob?.isActive == true)
            videoDetailsJob?.cancel()

        videoDetailsJob = viewModelScope.launch {
            val albumDetails = contentRepository.getAlbumDetails(video.contentId) ?: return@launch
            val firstVideo = albumDetails.videoList.firstOrNull() ?: return@launch
            val videoDetails = mediaRepository.getVideoDetails(firstVideo) ?: return@launch
            this@HomeViewModel.videoDetails.value = videoDetails
        }
    }

    fun getUserInfo() {
        viewModelScope.launch {
            userDetails.value = userRepository.getUserInfo() ?: return@launch
        }
    }
}