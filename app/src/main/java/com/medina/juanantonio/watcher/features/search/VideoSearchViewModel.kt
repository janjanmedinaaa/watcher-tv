package com.medina.juanantonio.watcher.features.search

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
class VideoSearchViewModel @Inject constructor(
    private val homePageRepository: IHomePageRepository
) : ViewModel() {

    val searchResults = MutableLiveData<Event<List<Video>>>()
    val videoMedia = MutableLiveData<Event<VideoMedia>>()
    val episodeList = MutableLiveData<Event<VideoGroup>>()
    private var job: Job? = null

    private var displaysEpisodes = false

    fun searchKeyword(keyword: String) {
        if (job?.isActive == true) job?.cancel()
        job = viewModelScope.launch {
            val results = homePageRepository.searchByKeyword(keyword)
            if (!results.isNullOrEmpty()) {
                searchResults.value = Event(results)
            }
        }
    }

    fun getVideo(video: Video) {
        if (job?.isActive == true) return
        job = viewModelScope.launch {
            val videoMedia = homePageRepository.getVideo(
                id = video.contentId,
                category = video.category ?: -1,
                episodeNumber = video.episodeId
            )
            videoMedia?.let {
                this@VideoSearchViewModel.videoMedia.value = Event(it)
            }
        }
    }

    fun handleSeries(video: Video) {
        if (displaysEpisodes) getVideo(video)
        else getEpisodeList(video)
    }

    private fun getEpisodeList(video: Video) {
        if (job?.isActive == true) return
        job = viewModelScope.launch {
            val videoMedia = homePageRepository.getSeriesEpisodes(video)
            videoMedia?.let {
                this@VideoSearchViewModel.episodeList.value = Event(it)
            }
        }
    }
}