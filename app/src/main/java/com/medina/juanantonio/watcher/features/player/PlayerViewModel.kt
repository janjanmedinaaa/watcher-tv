package com.medina.juanantonio.watcher.features.player

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medina.juanantonio.watcher.data.models.Video
import com.medina.juanantonio.watcher.data.models.VideoGroup
import com.medina.juanantonio.watcher.data.models.VideoMedia
import com.medina.juanantonio.watcher.features.loader.LoaderUseCase
import com.medina.juanantonio.watcher.shared.utils.Event
import com.medina.juanantonio.watcher.sources.content.WatchHistoryUseCase
import com.medina.juanantonio.watcher.sources.media.IMediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val mediaRepository: IMediaRepository,
    private val loaderUseCase: LoaderUseCase,
    private val watchHistoryUseCase: WatchHistoryUseCase
) : ViewModel(), PlaybackStateMachine {

    private var video: Video? = null
    private var job: Job? = null

    lateinit var videoMedia: VideoMedia

    val savedProgress = MutableLiveData<Event<Long>>()
    val handleVideoEndNavigation = MutableLiveData<Event<Unit>>()
    val episodeList = MutableLiveData<Event<VideoGroup>>()

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

    private var episodeNumbers = listOf<Int>()

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

    fun setEpisodeNumbers(episodeNumbers: List<Int>) {
        this.episodeNumbers = episodeNumbers
    }

    fun saveVideo(progress: Long) {
        viewModelScope.launch {
            video?.let {
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
        viewModelScope.launch {
            video?.let {
                if ((isLastEpisode && !it.isMovie) || it.isMovie) {
                    watchHistoryUseCase.removeOnGoingVideo(it.contentId)
                    handleVideoEndNavigation.value = Event(Unit)
                } else {
                    getNewVideoMedia(playNext = true)
                }
            }
        }
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

    /**
     * Used for handling Movie Recommendations, no need to
     * navigate to new PlayerFragment
     */
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
                    score = videoMedia.score
                }
                onStateChange(VideoPlaybackState.Load(it))
            }
            loaderUseCase.hide()
        }
    }

    /**
     * Used for handling Series Recommendations
     */
    fun getEpisodeList(video: Video) {
        if (job?.isActive == true) return
        job = viewModelScope.launch {
            loaderUseCase.show()
            val videoMedia = mediaRepository.getSeriesEpisodes(video)
            videoMedia?.let {
                this@PlayerViewModel.episodeList.value = Event(it)
            }
            loaderUseCase.hide()
        }
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
                    mediaRepository.currentlyPlayingVideo =
                        currentlyPlayingVideo.createNew(
                            episodeNumber = newEpisodeNumber,
                            videoProgress = 0L
                        )
                    onStateChange(VideoPlaybackState.Load(it))
                }
            }
            loaderUseCase.hide()
        }
    }
}