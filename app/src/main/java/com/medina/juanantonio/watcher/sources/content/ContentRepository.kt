package com.medina.juanantonio.watcher.sources.content

import com.medina.juanantonio.watcher.data.models.Video
import com.medina.juanantonio.watcher.data.models.VideoGroup
import com.medina.juanantonio.watcher.network.Result
import com.medina.juanantonio.watcher.network.models.home.HomePageBean
import com.medina.juanantonio.watcher.network.models.home.NavigationItemBean
import com.medina.juanantonio.watcher.shared.Constants.BannerProportions.CollectionProportion
import com.medina.juanantonio.watcher.shared.Constants.BannerProportions.MovieListProportion

class ContentRepository(
    private val remoteSource: IContentRemoteSource
) : IContentRepository {

    override val navigationItems: ArrayList<NavigationItemBean> = arrayListOf()
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
                addAll(filteredItems ?: emptyList())
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

        val pageHasContent = getHomePage(navigationId, startingPage)
        if (pageHasContent) {
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

    private suspend fun getHomePage(navigationId: Int?, page: Int): Boolean {
        val result = remoteSource.getHomePage(page, navigationId)

        return if (result is Result.Success) {
            val validVideoGroups = result.data?.data?.recommendItems?.filter {
                !it.recommendContentVOList.any { content ->
                    content.contentType == HomePageBean.ContentType.UNKNOWN
                } && getVideoGroupContentType(it) != null
            }

            val uniqueVideoGroups = validVideoGroups?.filter { bean ->
                val mapId = navigationId ?: -1
                homeContentMap[mapId]?.any { savedVideoGroups ->
                    savedVideoGroups.any {
                        it.category == bean.homeSectionName
                    }
                }?.not() ?: true
            }

            val listVideoGroup = uniqueVideoGroups?.map {
                VideoGroup(
                    category = it.homeSectionName,
                    videoList = it.recommendContentVOList.map { videoItem ->
                        Video(videoItem)
                    },
                    contentType = getVideoGroupContentType(it) ?: VideoGroup.ContentType.VIDEOS
                )
            }

            val mapId = navigationId ?: -1
            if (!listVideoGroup.isNullOrEmpty()) {
                if (homeContentMap[mapId] == null) {
                    homeContentMap[mapId] = arrayListOf(listVideoGroup)
                } else {
                    homeContentMap[mapId]?.add(listVideoGroup)
                }
            }

            !validVideoGroups.isNullOrEmpty()
        } else false
    }

    private fun getVideoGroupContentType(bean: HomePageBean): VideoGroup.ContentType? {
        val hasTitle =
            bean.recommendContentVOList.all { content -> content.title.isNotBlank() }

        return when (bean.homeSectionType) {
            HomePageBean.SectionType.SINGLE_ALBUM -> {
                VideoGroup.ContentType.VIDEOS
            }
            HomePageBean.SectionType.BLOCK_GROUP -> {
                if (hasTitle && bean.bannerProportion == 1.0) {
                    VideoGroup.ContentType.ARTISTS
                } else when (bean.bannerProportion) {
                    CollectionProportion -> VideoGroup.ContentType.COLLECTION
                    MovieListProportion -> VideoGroup.ContentType.MOVIE_LIST
                    1.0 -> VideoGroup.ContentType.TOP_CONTENT
                    else -> null
                }
            }
            else -> null
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

    override suspend fun getSearchLeaderboard(): List<Video>? {
        val result = remoteSource.getSearchLeaderboard()

        return if (result is Result.Success) {
            val data = result.data?.data ?: return null
            data.list.map { Video(it) }
        } else null
    }
}

interface IContentRepository {
    val navigationItems: List<NavigationItemBean>

    suspend fun setupNavigationBar()
    fun resetPage()
    suspend fun setupHomePage(navigationId: Int?, startingPage: Int = 0)
    fun getHomePage(): List<VideoGroup>
    fun clearHomePage()
    suspend fun getAlbumDetails(id: Int): VideoGroup?

    suspend fun searchByKeyword(keyword: String): List<Video>?
    suspend fun getSearchLeaderboard(): List<Video>?
}