package com.medina.juanantonio.watcher.sources.home

import com.medina.juanantonio.watcher.data.models.Video
import com.medina.juanantonio.watcher.data.models.VideoGroup
import com.medina.juanantonio.watcher.data.models.VideoMedia
import com.medina.juanantonio.watcher.network.Result
import com.medina.juanantonio.watcher.network.models.home.HomePageBean
import com.medina.juanantonio.watcher.network.models.player.EpisodeBean

class HomePageRepository(
    private val remoteSource: IHomePageRemoteSource,
    private val database: IHomePageDatabase
) : IHomePageRepository {

    override var currentlyPlayingVideo: Video? = null

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

    override suspend fun getVideo(id: Int, category: Int, episodeNumber: Int): VideoMedia? {
        val videoDetailsResult = remoteSource.getVideoDetails(id, category)

        return if (videoDetailsResult is Result.Success) {
            val videoDetails = videoDetailsResult.data?.data
            val episode = if (episodeNumber == 0) {
                videoDetails?.episodeVo?.firstOrNull()
            } else {
                videoDetails?.episodeVo?.firstOrNull { it.seriesNo == episodeNumber }
            }
            val definition = episode?.definitionList?.let {
                it.firstOrNull { definition ->
                    definition.code == EpisodeBean.DefinitionCode.GROOT_SD
                } ?: it.firstOrNull()
            }?.code

            val videoMediaResult = remoteSource.getVideoResource(
                category = category,
                contentId = id,
                episodeId = episode?.id ?: -1,
                definition = definition?.name ?: ""
            )

            if (videoMediaResult is Result.Success) {
                VideoMedia(
                    contentId = id,
                    categoryId = category,
                    episodeBean = episode ?: return null,
                    detailsResponse = videoDetailsResult.data?.data ?: return null,
                    mediaResponse = videoMediaResult.data?.data ?: return null
                )
            } else null
        } else null
    }

    override suspend fun getSeriesEpisodes(video: Video): VideoGroup? {
        val result = remoteSource.getVideoDetails(video.contentId, video.category ?: -1)

        return if (result is Result.Success) {
            val videoDetails = result.data?.data ?: return null
            VideoGroup(
                category = videoDetails.name,
                videoList = videoDetails.episodeVo.map { episodeBean ->
                    Video(video, episodeBean, videoDetails.episodeVo.size)
                }
            )
        } else null
    }

    override suspend fun searchByKeyword(keyword: String): List<Video>? {
        val result = remoteSource.searchByKeyword(keyword)

        return if (result is Result.Success) {
            val data = result.data?.data ?: return null
            val filteredList = data.searchResults.filter { it.dramaType != null }

            filteredList.map { Video(it) }
        } else null
    }

    override suspend fun addOnGoingVideo(video: Video) {
        database.addVideo(video)
    }

    override suspend fun getOnGoingVideos(): List<Video> {
        return database.getOnGoingVideos()
    }

    override suspend fun getOnGoingVideo(id: Int): Video? {
        return database.getVideo(id)
    }

    override suspend fun removeOnGoingVideo(id: Int) {
        database.removeVideo(id)
    }
}

interface IHomePageRepository {

    // TODO: Maybe move this to somewhere cleaner? Also reset value
    var currentlyPlayingVideo: Video?

    suspend fun setupHomePage(startingPage: Int = 0)
    fun getHomePage(): List<VideoGroup>
    fun clearHomePage()

    suspend fun getVideo(id: Int, category: Int, episodeNumber: Int = 0): VideoMedia?
    suspend fun getSeriesEpisodes(video: Video): VideoGroup?
    suspend fun searchByKeyword(keyword: String): List<Video>?

    suspend fun addOnGoingVideo(video: Video)
    suspend fun getOnGoingVideos(): List<Video>
    suspend fun getOnGoingVideo(id: Int): Video?
    suspend fun removeOnGoingVideo(id: Int)
}