package com.medina.juanantonio.watcher.sources.home

import com.medina.juanantonio.watcher.data.models.Video
import com.medina.juanantonio.watcher.data.models.VideoGroup
import com.medina.juanantonio.watcher.data.models.VideoMedia
import com.medina.juanantonio.watcher.network.Result
import com.medina.juanantonio.watcher.network.models.home.HomePageBean

class HomePageRepository(
    private val remoteSource: IHomePageRemoteSource
) : IHomePageRepository {

    override val homeContentList: ArrayList<VideoGroup> = arrayListOf()

    override suspend fun setupHomePage(startingPage: Int) {
        val result = getHomePage(startingPage)
        if (!result.isNullOrEmpty()) {
            setupHomePage(startingPage + 1)
        }
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

            homeContentList.addAll(listVideoGroup ?: emptyList())
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
            val definition = episode?.definitionList?.firstOrNull()?.code

            val videoMediaResult = remoteSource.getVideoResource(
                category = category,
                contentId = id,
                episodeId = episode?.id ?: -1,
                definition = definition?.name ?: ""
            )

            if (videoMediaResult is Result.Success) {
                VideoMedia(
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
                    Video(video, episodeBean)
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
}

interface IHomePageRepository {
    val homeContentList: ArrayList<VideoGroup>

    suspend fun setupHomePage(startingPage: Int = 0)

    suspend fun getVideo(id: Int, category: Int, episodeNumber: Int = 0): VideoMedia?
    suspend fun getSeriesEpisodes(video: Video): VideoGroup?
    suspend fun searchByKeyword(keyword: String): List<Video>?
}