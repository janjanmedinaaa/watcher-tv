package com.medina.juanantonio.watcher.features.search

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medina.juanantonio.watcher.data.models.video.Video
import com.medina.juanantonio.watcher.data.models.video.VideoGroup
import com.medina.juanantonio.watcher.data.models.video.VideoMedia
import com.medina.juanantonio.watcher.features.loader.LoaderUseCase
import com.medina.juanantonio.watcher.github.sources.IUpdateRepository
import com.medina.juanantonio.watcher.github.sources.IUpdateRepository.Companion.DEVELOPER_KEYWORD
import com.medina.juanantonio.watcher.shared.utils.Event
import com.medina.juanantonio.watcher.sources.content.IContentRepository
import com.medina.juanantonio.watcher.sources.media.IMediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VideoSearchViewModel @Inject constructor(
    private val contentRepository: IContentRepository,
    private val mediaRepository: IMediaRepository,
    private val updateRepository: IUpdateRepository,
    private val loaderUseCase: LoaderUseCase
) : ViewModel() {

    val searchResults = MutableLiveData<Event<List<VideoGroup>>>()
    val videoMedia = MutableLiveData<Event<VideoMedia>>()
    val episodeList = MutableLiveData<Event<VideoGroup>>()
    private var job: Job? = null

    private var displaysEpisodes = false

    val searchResultHint: String
        get() = contentRepository.searchResultsHint

    init {
        getLeaderboard()
    }

    fun searchKeyword(keyword: String) {
        if (job?.isActive == true) job?.cancel()
        if (keyword.isBlank()) {
            getLeaderboard()
            return
        }

        if (keyword.equals(DEVELOPER_KEYWORD, ignoreCase = true)) {
            getEnableDeveloperMode()
            return
        }

        job = viewModelScope.launch {
            val results = contentRepository.searchByKeyword(keyword)
            if (!results.isNullOrEmpty()) {
                searchResults.value = Event(results)
            }
        }
    }

    private fun getLeaderboard() {
        if (job?.isActive == true) job?.cancel()
        job = viewModelScope.launch {
            val results = contentRepository.getSearchLeaderboard()
            if (results?.videoList?.isNotEmpty() == true) {
                searchResults.value = Event(listOf(results))
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
                    score = videoMedia.score
                }
                this@VideoSearchViewModel.videoMedia.value = Event(it)
            }
            loaderUseCase.hide()
        }
    }

    fun handleSeries(video: Video) {
        if (displaysEpisodes) getVideoMedia(video)
        else getEpisodeList(video)
    }

    fun enableDeveloperMode() {
        viewModelScope.launch {
            updateRepository.enableDeveloperMode()
        }
    }

    private fun getEpisodeList(video: Video) {
        if (job?.isActive == true) return
        job = viewModelScope.launch {
            loaderUseCase.show()
            val videoMedia = mediaRepository.getSeriesEpisodes(video)
            videoMedia?.let {
                this@VideoSearchViewModel.episodeList.value = Event(it)
            }
            loaderUseCase.hide()
        }
    }

    private fun getEnableDeveloperMode() {
        val enableDeveloperModeItem = Video(
            category = 0,
            contentId = -1,
            imageUrl = "",
            title = "Enable Developer Mode"
        ).apply {
            enableDeveloperMode = true
        }
        val videoGroup = VideoGroup(
            category = "Enable Developer Mode",
            videoList = listOf(enableDeveloperModeItem),
            contentType = VideoGroup.ContentType.VIDEOS
        )

        searchResults.value = Event(listOf(videoGroup))
    }
}