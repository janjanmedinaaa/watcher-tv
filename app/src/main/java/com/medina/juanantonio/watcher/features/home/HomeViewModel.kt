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

    private var displaysEpisodes = false
    var contentLoaded = false

    private var job: Job? = null

    private suspend fun setupHomePage(page: Int) {
        val currentPage = homePageRepository.getHomePage(page = page)
        currentPage?.let {
            contentList.value = Event(it)
            setupHomePage(page = page + 1)
        }
    }

    fun setupVideoList(episodeList: VideoGroup?) {
        if (contentLoaded) return
        contentLoaded = true
        if (episodeList != null) {
            displaysEpisodes = true
            contentList.value = Event(listOf(episodeList))
        }
        else viewModelScope.launch {
            setupHomePage(page = 0)
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
                this@HomeViewModel.videoMedia.value = Event(it)
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
                this@HomeViewModel.episodeList.value = Event(it)
            }
        }
    }
}