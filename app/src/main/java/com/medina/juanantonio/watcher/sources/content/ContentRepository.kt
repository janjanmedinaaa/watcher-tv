package com.medina.juanantonio.watcher.sources.content

import android.content.Context
import com.medina.juanantonio.watcher.R
import com.medina.juanantonio.watcher.data.manager.IDataStoreManager
import com.medina.juanantonio.watcher.data.models.video.Video
import com.medina.juanantonio.watcher.data.models.video.VideoGroup
import com.medina.juanantonio.watcher.network.Result
import com.medina.juanantonio.watcher.network.models.home.HomePageBean
import com.medina.juanantonio.watcher.network.models.home.NavigationItemBean
import com.medina.juanantonio.watcher.shared.Constants.BannerProportions.CollectionProportion
import com.medina.juanantonio.watcher.shared.Constants.BannerProportions.MovieListProportion
import com.medina.juanantonio.watcher.shared.extensions.toastIfNotBlank
import com.medina.juanantonio.watcher.sources.content.IContentRepository.Companion.API_HEADERS

class ContentRepository(
    private val context: Context,
    private val remoteSource: IContentRemoteSource,
    private val dataStoreManager: IDataStoreManager,
    private val likedVideoUseCase: LikedVideoUseCase
) : IContentRepository {

    companion object {
        const val MY_LIST_NAVIGATION_BAR_ID = 11399
    }

    override val navigationItems: ArrayList<NavigationItemBean> = arrayListOf()
    override var searchResultsHint: String = ""
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
                add(
                    NavigationItemBean(
                        id = MY_LIST_NAVIGATION_BAR_ID,
                        name = context.getString(R.string.my_list_navigation_title),
                        redirectContentType = NavigationItemBean.RedirectContentType.HOME,
                        sequence = 10
                    )
                )
            }
        } else {
            result.message.toastIfNotBlank(context)
        }
    }

    override fun resetPage() {
        currentPage = 0
    }

    override fun setPageId(id: Int) {
        currentNavigationPage = id
    }

    override suspend fun setupPage(
        navigationId: Int,
        startingPage: Int,
        onFirstPage: () -> Unit
    ) {
        if (startingPage == 0 && homeContentMap[navigationId] != null) {
            onFirstPage()
            return
        }

        val pageHasContent = getHomePage(navigationId, startingPage)
        if (pageHasContent) {
            if (startingPage == 0) onFirstPage()
            setupPage(navigationId, startingPage + 1)
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
        if (navigationId == MY_LIST_NAVIGATION_BAR_ID) return getMyListNavigation(page)

        val result = remoteSource.getHomePage(page, navigationId)

        return if (result is Result.Success) {
            val validVideoGroups = result.data?.data?.recommendItems?.filter {
                !it.recommendContentVOList.any { content ->
                    (content.contentType == HomePageBean.ContentType.UNKNOWN) || content.needLogin
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

            searchResultsHint = result.data?.data?.searchKeyWord ?: ""

            !validVideoGroups.isNullOrEmpty()
        } else false
    }

    private suspend fun getMyListNavigation(page: Int): Boolean {
        val likedVideos = likedVideoUseCase.getLikedVideos()

        return if (likedVideos.isNotEmpty()) {
            val listVideoGroup = arrayListOf<VideoGroup>()
            when (page) {
                0 -> {
                    listVideoGroup.add(
                        VideoGroup(
                            category = context.getString(R.string.my_list_navigation_title),
                            videoList = likedVideos.map { likedVideo ->
                                Video(likedVideo)
                            },
                            contentType = VideoGroup.ContentType.VIDEOS
                        )
                    )
                }
            }

            val mapId = MY_LIST_NAVIGATION_BAR_ID
            if (listVideoGroup.isNotEmpty()) {
                if (homeContentMap[mapId] == null) {
                    homeContentMap[mapId] = arrayListOf(listVideoGroup)
                } else {
                    homeContentMap[mapId]?.add(listVideoGroup)
                }
            }

            listVideoGroup.isNotEmpty()
        } else false
    }

    private fun getVideoGroupContentType(bean: HomePageBean): VideoGroup.ContentType? {
        val hasTitle =
            bean.recommendContentVOList.all { content -> content.title.isNotBlank() }

        return when (bean.homeSectionType) {
            HomePageBean.SectionType.SINGLE_ALBUM -> {
                VideoGroup.ContentType.VIDEOS
            }
            HomePageBean.SectionType.MOVIE_RESERVE -> {
                VideoGroup.ContentType.COMING_SOON
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

    override suspend fun getHeaders() {
        val result = remoteSource.getHeaders()
        if (result is Result.Success && result.data?.isJsonObject == true) {
            dataStoreManager.putString(API_HEADERS, result.data.toString())
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
        } else {
            result.message.toastIfNotBlank(context)
            null
        }
    }

    override suspend fun searchByKeyword(keyword: String): List<VideoGroup>? {
        val result = remoteSource.searchByKeyword(keyword)

        return if (result is Result.Success) {
            val data = result.data?.data ?: return null
            val filteredSearchList = data.searchResults.filter { it.coverVerticalUrl.isNotBlank() }
            val filteredAlbumList = data.albumResults.map { album ->
                album.contents.filter { it.coverVerticalUrl.isNotBlank() }
                album
            }

            arrayListOf<VideoGroup>().apply {
                val searchResultsVideoGroup =
                    VideoGroup(
                        category = context.getString(R.string.search_results, keyword),
                        videoList = filteredSearchList.map { Video(it) },
                        contentType = VideoGroup.ContentType.VIDEOS
                    )
                add(searchResultsVideoGroup)

                filteredAlbumList.forEach { album ->
                    val videoList = album.contents.map { Video(it) }
                    val videoGroup =
                        VideoGroup(
                            category = album.name,
                            videoList = videoList,
                            contentType = VideoGroup.ContentType.VIDEOS
                        )
                    add(videoGroup)
                }
            }
        } else {
            result.message.toastIfNotBlank(context)
            null
        }
    }

    override suspend fun searchByKeywordSpecific(title: String, year: String): Video? {
        val result = remoteSource.searchByKeyword(title)

        return if (result is Result.Success) {
            val data = result.data?.data ?: return null
            val filteredSearchList = data.searchResults.filter { it.coverVerticalUrl.isNotBlank() }
            filteredSearchList
                .map { Video(it) }
                .firstOrNull {
                    val (name, _) = it.getSeriesTitleDescription()
                    name.equals(title, true) && it.year == year
                }
        } else null
    }

    override suspend fun getSearchLeaderboard(): VideoGroup? {
        val result = remoteSource.getSearchLeaderboard()

        return if (result is Result.Success) {
            val data = result.data?.data ?: return null
            VideoGroup(
                category = context.getString(R.string.search_leaderboard),
                videoList = data.list.map { Video(it) },
                contentType = VideoGroup.ContentType.LEADERBOARD
            )
        } else null
    }
}

interface IContentRepository {
    val navigationItems: List<NavigationItemBean>
    var searchResultsHint: String

    suspend fun setupNavigationBar()
    fun resetPage()
    fun setPageId(id: Int)
    suspend fun setupPage(
        navigationId: Int,
        startingPage: Int = 0,
        onFirstPage: () -> Unit = {}
    )

    fun getHomePage(): List<VideoGroup>
    fun clearHomePage()

    suspend fun getHeaders()

    suspend fun getAlbumDetails(id: Int): VideoGroup?

    suspend fun searchByKeyword(keyword: String): List<VideoGroup>?
    suspend fun searchByKeywordSpecific(title: String, year: String): Video?
    suspend fun getSearchLeaderboard(): VideoGroup?

    companion object {
        const val API_HEADERS = "API_HEADERS"
    }
}