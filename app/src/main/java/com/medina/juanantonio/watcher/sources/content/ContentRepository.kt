package com.medina.juanantonio.watcher.sources.content

import com.medina.juanantonio.watcher.data.models.Video
import com.medina.juanantonio.watcher.data.models.VideoGroup
import com.medina.juanantonio.watcher.network.Result
import com.medina.juanantonio.watcher.network.models.home.HomePageBean
import com.medina.juanantonio.watcher.network.models.home.NavigationItemBean
import com.medina.juanantonio.watcher.sources.media.IVideoDatabase

class ContentRepository(
    private val remoteSource: IContentRemoteSource,
    private val database: IVideoDatabase
) : IContentRepository {

    override val navigationItems: ArrayList<Video> = arrayListOf()
    private var currentNavigationPage = -1

    private val homeContentMap: MutableMap<Int, ArrayList<List<VideoGroup>>> = mutableMapOf()
    private var currentPage = 0

    override suspend fun setupNavigationBar() {
        val result = remoteSource.getNavigationBar()

        if (result is Result.Success) {
            val sortedItems =
                result.data?.data?.navigationBarItemList?.sortedBy { it.sequence }
            val filteredItems = sortedItems?.filter {
                it.redirectContentType == NavigationItemBean.RedirectContentType.HOME
            }

            navigationItems.apply {
                clear()
                addAll(filteredItems?.map { Video(it) } ?: emptyList())
            }
        }
    }

    override fun resetPage() {
        currentPage = 0
    }

    override suspend fun setupHomePage(navigationId: Int?, startingPage: Int) {
        if (startingPage == 0 && homeContentMap[navigationId] != null) {
            currentNavigationPage = navigationId ?: -1
            return
        }

        val result = getHomePage(navigationId, startingPage)
        if (!result.isNullOrEmpty()) {
            currentNavigationPage = navigationId ?: -1
            setupHomePage(navigationId, startingPage + 1)
        }
    }

    override fun getHomePage(): List<VideoGroup> {
        val page = homeContentMap[currentNavigationPage]?.getOrNull(currentPage)
        return if (!page.isNullOrEmpty()) {
            currentPage++
            page
        } else emptyList()
    }

    override fun clearHomePage() {
        resetPage()
        homeContentMap.clear()
    }

    private suspend fun getHomePage(navigationId: Int?, page: Int): List<VideoGroup>? {
        val result = remoteSource.getHomePage(page, navigationId)

        return if (result is Result.Success) {
            val filteredVideos = result.data?.data?.recommendItems?.filter {
                val areContentsValid =
                    it.recommendContentVOList.all { content -> content.title.isNotBlank() }

                it.homeSectionType == HomePageBean.SectionType.SINGLE_ALBUM ||
                        (it.homeSectionType == HomePageBean.SectionType.BLOCK_GROUP && areContentsValid)
            }

            val listVideoGroup = filteredVideos?.map {
                VideoGroup(
                    category = it.homeSectionName,
                    videoList = it.recommendContentVOList.map { videoItem ->
                        Video(videoItem)
                    },
                    contentType = getVideoGroupContentType(it)
                )
            }

            val mapId = navigationId ?: -1
            val mapItem = listVideoGroup ?: emptyList()
            if (homeContentMap[mapId] == null) {
                homeContentMap[mapId] = arrayListOf(mapItem)
            } else {
                homeContentMap[mapId]?.add(mapItem)
            }

            listVideoGroup
        } else null
    }

    private fun getVideoGroupContentType(bean: HomePageBean): VideoGroup.ContentType {
        val areContentsValid =
            bean.recommendContentVOList.all { content -> content.title.isNotBlank() }

        return when {
            bean.homeSectionType == HomePageBean.SectionType.SINGLE_ALBUM -> {
                VideoGroup.ContentType.VIDEOS
            }
            bean.homeSectionType == HomePageBean.SectionType.BLOCK_GROUP
                    && areContentsValid -> {
                VideoGroup.ContentType.ARTISTS
            }
            else -> VideoGroup.ContentType.VIDEOS
        }
    }

    override suspend fun getAlbumDetails(id: Int): VideoGroup? {
        val result = remoteSource.getAlbumDetails(id = id)

        return if (result is Result.Success) {
            val resultData = result.data?.data

            VideoGroup(
                category = resultData?.name ?: "",
                videoList = resultData?.content?.map { videoItem ->
                    Video(videoItem)
                } ?: emptyList(),
                contentType = VideoGroup.ContentType.VIDEOS
            )
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
    val navigationItems: List<Video>

    suspend fun setupNavigationBar()
    fun resetPage()
    suspend fun setupHomePage(navigationId: Int?, startingPage: Int = 0)
    fun getHomePage(): List<VideoGroup>
    fun clearHomePage()
    suspend fun getAlbumDetails(id: Int): VideoGroup?

    suspend fun searchByKeyword(keyword: String): List<Video>?
    suspend fun getOnGoingVideos(): List<Video>
    suspend fun getSearchLeaderboard(): List<Video>?
}