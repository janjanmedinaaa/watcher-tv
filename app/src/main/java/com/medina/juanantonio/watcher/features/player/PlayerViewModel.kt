package com.medina.juanantonio.watcher.features.player

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medina.juanantonio.watcher.data.models.settings.SettingsNumberPickerItem
import com.medina.juanantonio.watcher.data.models.settings.SettingsSelectionItem
import com.medina.juanantonio.watcher.data.models.video.LikedVideo
import com.medina.juanantonio.watcher.data.models.video.Video
import com.medina.juanantonio.watcher.data.models.video.VideoGroup
import com.medina.juanantonio.watcher.data.models.video.VideoMedia
import com.medina.juanantonio.watcher.di.ApplicationScope
import com.medina.juanantonio.watcher.features.loader.LoaderUseCase
import com.medina.juanantonio.watcher.shared.utils.Event
import com.medina.juanantonio.watcher.sources.content.LikedVideoUseCase
import com.medina.juanantonio.watcher.sources.content.WatchHistoryUseCase
import com.medina.juanantonio.watcher.sources.media.IMediaRepository
import com.medina.juanantonio.watcher.sources.settings.SettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val mediaRepository: IMediaRepository,
    private val loaderUseCase: LoaderUseCase,
    private val watchHistoryUseCase: WatchHistoryUseCase,
    private val settingsUseCase: SettingsUseCase,
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val likedVideoUseCase: LikedVideoUseCase
) : ViewModel(), PlaybackStateMachine {

    private var video: Video? = null
    private var job: Job? = null

    var videoMedia: VideoMedia = mediaRepository.currentlyPlayingVideoMedia!!

    val ldVideoMedia: LiveData<Event<VideoMedia>>
        get() = mediaRepository.videoMediaLiveData

    val selectedSelectionItem: LiveData<Event<SettingsSelectionItem>>
        get() = settingsUseCase.selectedSelectionItem

    val selectedNumberPickerItem: LiveData<Event<SettingsNumberPickerItem>>
        get() = settingsUseCase.selectedNumberPickerItem

    val selectedLanguage: String
        get() = settingsUseCase.selectedLanguage

    val selectedPlaybackSpeed: String
        get() = settingsUseCase.selectedPlaybackSpeed

    val selectedCaptionSize: Int
        get() = settingsUseCase.selectedCaptionSize

    val savedProgress = MutableLiveData<Event<Long>>()
    val handleVideoEndNavigation = MutableLiveData<Event<Unit>>()
    val episodeList = MutableLiveData<Event<Pair<VideoGroup, Boolean>>>()

    val isFirstEpisode: Boolean
        get() = video?.let { v ->
            val currentEpisodeIndex = episodeNumbers.indexOf(v.episodeNumber)
            currentEpisodeIndex == 0
        } ?: false

    private val isLastEpisode: Boolean
        get() = video?.let { v ->
            val currentEpisodeIndex = episodeNumbers.indexOf(v.episodeNumber)
            currentEpisodeIndex == episodeNumbers.lastIndex
        } ?: false

    val videoTitle: String
        get() = video?.title ?: ""

    private var episodeNumbers = listOf<Int>()

    private val playbackStateListeners = arrayListOf<PlaybackStateListener>()

    init {
        viewModelScope.launch {
            watchHistoryUseCase.getOnGoingVideos()
        }
    }

    fun addPlaybackStateListener(listener: PlaybackStateListener) {
        playbackStateListeners.add(listener)
    }

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

    fun setEpisodeNumbers(episodeNumbers: List<Int>) {
        this.episodeNumbers = episodeNumbers
    }

    fun saveVideo(progress: Long) {
        applicationScope.launch {
            video?.let {
                if (videoMedia.isComingSoon) return@let
                watchHistoryUseCase.addOnGoingVideo(
                    it.apply {
                        videoProgress = progress
                        lastWatchTime = System.currentTimeMillis()
                    },
                    videoMedia
                )
            }
        }
    }

    fun handleSkipPrevious() {
        getNewVideoMedia(playNext = false)
    }

    fun handleVideoEnd() {
        applicationScope.launch {
            video?.let { video ->
                if ((isLastEpisode && !video.isMovie) || video.isMovie) {
                    watchHistoryUseCase.removeOnGoingVideo(video)
                    handleVideoEndNavigation(video)
                } else {
                    getNewVideoMedia(playNext = true)
                }
            }
        }
    }

    private suspend fun handleVideoEndNavigation(video: Video) {
        if (!video.isMovie) {
            val nextSeason = videoMedia.connectedVideos?.find {
                it.seriesNo == (videoMedia.seriesNo ?: 0) + 1
            }

            if (nextSeason != null) {
                delay(500)
                getEpisodeList(Video(nextSeason), autoPlay = true)
                return
            }
        }
        handleVideoEndNavigation.postValue(Event(Unit))
    }

    fun getVideoDetails(id: Int) {
        viewModelScope.launch {
            val onGoingVideo = watchHistoryUseCase.getOnGoingVideo(id)
            val currentlyPlayingVideo = mediaRepository.currentlyPlayingVideo

            // Check if the user played a different episode in an on going series
            val isSameEpisode =
                onGoingVideo?.episodeNumber == currentlyPlayingVideo?.episodeNumber

            video = if (isSameEpisode) onGoingVideo else currentlyPlayingVideo
            savedProgress.value = Event(video?.videoProgress ?: 0L)
        }
    }

    fun getVideoMediaOfNewDefinition(definition: String?) {
        video?.let { getVideoMedia(it, definition) }
    }

    /**
     * Used for handling Movie Recommendations, no need to
     * navigate to new PlayerFragment
     */
    fun getVideoMedia(video: Video, definition: String? = null) {
        if (job?.isActive == true) return
        job = viewModelScope.launch {
            loaderUseCase.show()
            val videoMedia = mediaRepository.getVideo(
                id = video.contentId,
                category = video.category ?: -1,
                definition = definition,
                episodeNumber = video.episodeNumber
            )
            videoMedia?.let {
                val currentlyPlayingVideo = video.apply {
                    score = videoMedia.score
                }
                mediaRepository.setCurrentlyPlaying(currentlyPlayingVideo, it)
            }
            loaderUseCase.hide()
        }
    }

    /**
     * Used for handling Series Recommendations
     */
    fun getEpisodeList(video: Video, autoPlay: Boolean) {
        if (job?.isActive == true) return
        job = viewModelScope.launch {
            loaderUseCase.show()
            val videoMedia = mediaRepository.getSeriesEpisodes(video)
            videoMedia?.let {
                this@PlayerViewModel.episodeList.value = Event(Pair(it, autoPlay))
            }
            loaderUseCase.hide()
        }
    }

    suspend fun checkLikedVideo(): Boolean {
        return likedVideoUseCase.checkLikedVideo(videoMedia.contentId)
    }

    fun updateLikedVideo() {
        viewModelScope.launch {
            val id = videoMedia.contentId
            likedVideoUseCase.run {
                if (checkLikedVideo(id)) removeLikedVideo(id)
                else addLikedVideo(id)
            }
        }
    }

    fun getLikedVideo(id: Int): Flow<LikedVideo?> {
        return likedVideoUseCase.getLikedVideo(id)
    }

    private fun getNewEpisodeNumber(currentEpisode: Int, next: Boolean): Int {
        val currentEpisodeIndex = episodeNumbers.indexOf(currentEpisode)
        return if (next) episodeNumbers[currentEpisodeIndex + 1]
        else episodeNumbers[currentEpisodeIndex - 1]
    }

    /**
     * Used for getting the previous or next VideoMedia
     */
    private fun getNewVideoMedia(playNext: Boolean) {
        if (job?.isActive == true) return
        job = viewModelScope.launch {
            loaderUseCase.show()
            video?.let { currentlyPlayingVideo ->
                val newEpisodeNumber =
                    getNewEpisodeNumber(currentlyPlayingVideo.episodeNumber, playNext)

                val videoMedia = mediaRepository.getVideo(
                    id = currentlyPlayingVideo.contentId,
                    category = currentlyPlayingVideo.category ?: 1,
                    episodeNumber = newEpisodeNumber
                )

                videoMedia?.let {
                    val newPlayingVideo = currentlyPlayingVideo.createNew(
                        episodeNumber = newEpisodeNumber,
                        videoProgress = 0L
                    )
                    mediaRepository.setCurrentlyPlaying(newPlayingVideo, it)
                }
            }
            loaderUseCase.hide()
        }
    }
}