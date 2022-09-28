package com.medina.juanantonio.watcher.features.search

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medina.juanantonio.watcher.data.models.Video
import com.medina.juanantonio.watcher.data.models.VideoGroup
import com.medina.juanantonio.watcher.data.models.VideoMedia
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
    private val mediaRepository: IMediaRepository
) : ViewModel() {

    val searchResults = MutableLiveData<Event<List<Video>>>()
    val videoMedia = MutableLiveData<Event<VideoMedia>>()
    val episodeList = MutableLiveData<Event<VideoGroup>>()
    private var job: Job? = null

    private var displaysEpisodes = false

    init {
        getLeaderboard()
    }

    fun searchKeyword(keyword: String) {
        if (job?.isActive == true) job?.cancel()
        if (keyword.isBlank()) {
            getLeaderboard()
            return
        }

        job = viewModelScope.launch {
            val results = contentRepository.searchByKeyword(keyword)
            if (!results.isNullOrEmpty()) {
                searchResults.value = Event(results.sortedBy { it.title.trim() })
            }
        }
    }

    private fun getLeaderboard() {
        if (job?.isActive == true) job?.cancel()
        job = viewModelScope.launch {
            val results = contentRepository.getSearchLeaderboard()
            if (!results.isNullOrEmpty()) {
                searchResults.value = Event(results.sortedBy { it.title.trim() })
            }
        }
    }

    fun getVideoMedia(video: Video) {
        if (job?.isActive == true) return
        job = viewModelScope.launch {
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
        }
    }

    fun handleSeries(video: Video) {
        if (displaysEpisodes) getVideoMedia(video)
        else getEpisodeList(video)
    }

    private fun getEpisodeList(video: Video) {
        if (job?.isActive == true) return
        job = viewModelScope.launch {
            val videoMedia = mediaRepository.getSeriesEpisodes(video)
            videoMedia?.let {
                this@VideoSearchViewModel.episodeList.value = Event(it)
            }
        }
    }
}