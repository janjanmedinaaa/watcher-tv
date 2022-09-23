package com.medina.juanantonio.watcher.sources.media

import com.medina.juanantonio.watcher.data.models.Video
import com.medina.juanantonio.watcher.data.models.VideoGroup
import com.medina.juanantonio.watcher.data.models.VideoMedia
import com.medina.juanantonio.watcher.network.Result

class MediaRepository(
    private val remoteSource: IMediaRemoteSource,
    private val database: IVideoDatabase
) : IMediaRepository {

    override var currentlyPlayingVideo: Video? = null

    override suspend fun getVideo(id: Int, category: Int, episodeNumber: Int): VideoMedia? {
        val videoDetailsResult = remoteSource.getVideoDetails(id, category)

        return if (videoDetailsResult is Result.Success) {
            val videoDetails = videoDetailsResult.data?.data
            val episode = if (episodeNumber == 0) {
                videoDetails?.episodeVo?.firstOrNull()
            } else {
                videoDetails?.episodeVo?.firstOrNull { it.seriesNo == episodeNumber }
            }
            val definition = episode?.getDefinition()

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
                    mediaResponse = videoMediaResult.data?.data ?: return null,
                    score = videoDetails?.score ?: return null
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
                    Video(video, episodeBean, videoDetails.episodeVo.size, videoDetails.score)
                }
            )
        } else null
    }

    override suspend fun addOnGoingVideo(video: Video) {
        database.addVideo(video)
    }

    override suspend fun getOnGoingVideo(id: Int): Video? {
        return database.getVideo(id)
    }

    override suspend fun removeOnGoingVideo(id: Int) {
        database.removeVideo(id)
    }
}

interface IMediaRepository {
    // TODO: Maybe move this to somewhere cleaner? Also reset value
    var currentlyPlayingVideo: Video?

    suspend fun getVideo(id: Int, category: Int, episodeNumber: Int = 0): VideoMedia?
    suspend fun getSeriesEpisodes(video: Video): VideoGroup?
    suspend fun addOnGoingVideo(video: Video)
    suspend fun getOnGoingVideo(id: Int): Video?
    suspend fun removeOnGoingVideo(id: Int)
}