package com.medina.juanantonio.watcher.sources.content

import com.medina.juanantonio.watcher.data.models.Video
import com.medina.juanantonio.watcher.data.models.VideoGroup
import com.medina.juanantonio.watcher.network.Result
import com.medina.juanantonio.watcher.network.models.home.HomePageBean
import com.medina.juanantonio.watcher.sources.media.IVideoDatabase

class ContentRepository(
    private val remoteSource: IContentRemoteSource,
    private val database: IVideoDatabase
) : IContentRepository {

    private val homeContentList: ArrayList<List<VideoGroup>> = arrayListOf()

    private var currentPage = 0

    override suspend fun setupHomePage(startingPage: Int) {
        val result = getHomePage(startingPage)
        if (!result.isNullOrEmpty()) {
            setupHomePage(startingPage + 1)
        }
    }

    override fun getHomePage(): List<VideoGroup> {
        val page = homeContentList.getOrNull(currentPage)
        return if (!page.isNullOrEmpty()) {
            currentPage++
            page
        } else emptyList()
    }

    override fun clearHomePage() {
        currentPage = 0
        homeContentList.clear()
    }

    private suspend fun getHomePage(page: Int): List<VideoGroup>? {
        val result = remoteSource.getHomePage(page)

        return if (result is Result.Success) {
            val filteredVideos = result.data?.data?.recommendItems?.filter {
                it.homeSectionType == HomePageBean.SectionType.SINGLE_ALBUM
            }

            val listVideoGroup = filteredVideos?.map {
                VideoGroup(
                    category = it.homeSectionName,
                    videoList = it.recommendContentVOList.map { videoItem ->
                        Video(videoItem)
                    }
                )
            }

            homeContentList.add(listVideoGroup ?: emptyList())
            listVideoGroup
        } else null
    }

    override suspend fun searchByKeyword(keyword: String): List<Video>? {
        val result = remoteSource.searchByKeyword(keyword)

        return if (result is Result.Success) {
            val data = result.data?.data ?: return null
            val filteredList = data.searchResults.filter { it.coverVerticalUrl.isNotBlank() }

            filteredList.map { Video(it) }
        } else null
    }

    override suspend fun getOnGoingVideos(): List<Video> {
        return database.getOnGoingVideos()
    }

    override suspend fun getSearchLeaderboard(): List<Video>? {
        val result = remoteSource.getSearchLeaderboard()

        return if (result is Result.Success) {
            val data = result.data?.data ?: return null
            data.list.map { Video(it) }
        } else null
    }
}

interface IContentRepository {
    suspend fun setupHomePage(startingPage: Int = 0)
    fun getHomePage(): List<VideoGroup>
    fun clearHomePage()

    suspend fun searchByKeyword(keyword: String): List<Video>?
    suspend fun getOnGoingVideos(): List<Video>
    suspend fun getSearchLeaderboard(): List<Video>?
}