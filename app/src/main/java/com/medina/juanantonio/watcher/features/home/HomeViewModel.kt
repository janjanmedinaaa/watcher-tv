package com.medina.juanantonio.watcher.features.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medina.juanantonio.watcher.data.models.video.Video
import com.medina.juanantonio.watcher.data.models.video.VideoGroup
import com.medina.juanantonio.watcher.data.models.video.VideoMedia
import com.medina.juanantonio.watcher.di.ApplicationScope
import com.medina.juanantonio.watcher.features.loader.LoaderUseCase
import com.medina.juanantonio.watcher.network.models.auth.GetUserInfoResponse
import com.medina.juanantonio.watcher.network.models.home.NavigationItemBean
import com.medina.juanantonio.watcher.network.models.player.GetVideoDetailsResponse
import com.medina.juanantonio.watcher.shared.Constants.VideoGroupTitle.ContinueWatchingTitle
import com.medina.juanantonio.watcher.shared.utils.Event
import com.medina.juanantonio.watcher.sources.auth.AuthUseCase
import com.medina.juanantonio.watcher.sources.auth.IAuthRepository
import com.medina.juanantonio.watcher.sources.content.IContentRepository
import com.medina.juanantonio.watcher.sources.content.WatchHistoryUseCase
import com.medina.juanantonio.watcher.sources.media.IMediaRepository
import com.medina.juanantonio.watcher.sources.user.IUserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val contentRepository: IContentRepository,
    private val mediaRepository: IMediaRepository,
    private val loaderUseCase: LoaderUseCase,
    private val watchHistoryUseCase: WatchHistoryUseCase,
    private val userRepository: IUserRepository,
    private val authRepository: IAuthRepository,
    private val authUseCase: AuthUseCase
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

    private val _showLogoutDialog = MutableLiveData<Event<Unit>>()
    val showLogoutDialog: LiveData<Event<Unit>>
        get() = _showLogoutDialog

    private val _navigateToHomeScreen = MutableLiveData<Event<Unit>>()
    val navigateToHomeScreen: LiveData<Event<Unit>>
        get() = _navigateToHomeScreen

    private var isDisplayingEpisodes = false
    var isContentLoaded = false

    private var job: Job? = null
    private var videoDetailsJob: Job? = null

    fun setupVideoList(videoGroup: VideoGroup?, autoPlayFirstEpisode: Boolean) {
        if (isContentLoaded) {
            if (videoGroup == null) viewModelScope.launch {
                getOnGoingVideoGroup()
            }
            return
        }
        isContentLoaded = true

        viewModelScope.launch {
            if (videoGroup != null) {
                contentList.value = Event(listOf(videoGroup))

                val areAlbumItems = videoGroup.videoList.any { it.isAlbumItem }
                if (!areAlbumItems) {
                    isDisplayingEpisodes = true

                    if (autoPlayFirstEpisode) {
                        autoPlayFirstEpisode(videoGroup)
                    } else {
                        autoPlayOngoingVideo(videoGroup)
                    }
                }

                return@launch
            }

            getOnGoingVideoGroup()
            contentList.value = Event(contentRepository.getHomePage())
        }
    }

    private fun autoPlayFirstEpisode(videoGroup: VideoGroup) {
        videoGroup.videoList.firstOrNull {
            it.episodeNumber == 1
        }?.let { episodeToPlay ->
            episodeToAutoPlay.value = Event(episodeToPlay)
        }
    }

    private suspend fun autoPlayOngoingVideo(videoGroup: VideoGroup) {
        watchHistoryUseCase.getOnGoingVideos()
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
    }

    fun getVideoMediaFromId(searchId: String) {
        val splitSearchId = searchId.split(";")
        val category = splitSearchId.getOrNull(0)?.toIntOrNull() ?: return
        val contentId = splitSearchId.getOrNull(1)?.toIntOrNull() ?: -1
        val imageUrl = splitSearchId.getOrNull(2).toString()
        val title = splitSearchId.getOrNull(3).toString()

        getVideoMedia(
            Video(
                category = category,
                contentId = contentId,
                imageUrl = imageUrl,
                title = title
            )
        )
    }

    fun getVideoMedia(video: Video, isComingSoon: Boolean = false) {
        if (job?.isActive == true) return
        job = viewModelScope.launch {
            loaderUseCase.show()
            val videoMedia = mediaRepository.getVideo(
                id = video.contentId,
                category = video.category ?: -1,
                episodeNumber = video.episodeNumber,
                isComingSoon = isComingSoon
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
        val loadHomePageContent = applicationScope.launch {
            loaderUseCase.show()
            contentRepository.setPageId(id)
            contentRepository.setupPage(id) {
                contentRepository.resetPage()
                removeNavigationContent()
                addNewContent()
                loaderUseCase.hide()
            }
        }
        loadHomePageContent.invokeOnCompletion {
            loaderUseCase.hide()
        }
    }

    private fun removeNavigationContent() {
        removeNavigationContent.postValue(Event(Unit))
    }

    fun addNewContent() {
        contentList.postValue(Event(contentRepository.getHomePage()))
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

    fun handleLogoActions() {
        viewModelScope.launch {
            val isLoggedIn = authRepository.isUserAuthenticated()
            if (isLoggedIn) {
                _showLogoutDialog.value = Event(Unit)
            } else {
                authRepository.continueWithoutAuth(false)
                _navigateToHomeScreen.value = Event(Unit)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            val userId = userDetails.value?.userId ?: ""
            val isSuccessful = authUseCase.logout(userId)
            if (isSuccessful) {
                watchHistoryUseCase.clearLocalOnGoingVideos()
                _navigateToHomeScreen.value = Event(Unit)
            }
        }
    }

    fun saveCacheVideos() {
        applicationScope.launch {
            val cacheVideos = watchHistoryUseCase.getLocalOnGoingVideos()
            cacheVideos.forEach {
                val videoMedia = mediaRepository.getVideo(
                    it.contentId,
                    it.category ?: 0,
                    it.episodeNumber
                ) ?: return@forEach

                watchHistoryUseCase.addOnGoingVideo(it, videoMedia)
            }
        }
    }

    fun clearCacheVideos() {
        viewModelScope.launch {
            watchHistoryUseCase.clearLocalOnGoingVideos()
        }
    }
}