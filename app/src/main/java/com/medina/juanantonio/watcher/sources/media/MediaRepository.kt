package com.medina.juanantonio.watcher.sources.media

import android.content.Context
import android.widget.Toast
import com.medina.juanantonio.watcher.data.models.video.Video
import com.medina.juanantonio.watcher.data.models.video.VideoGroup
import com.medina.juanantonio.watcher.data.models.video.VideoMedia
import com.medina.juanantonio.watcher.network.Result
import com.medina.juanantonio.watcher.network.models.player.GetVideoDetailsResponse

class MediaRepository(
    private val context: Context,
    private val remoteSource: IMediaRemoteSource
) : IMediaRepository {

    override var currentlyPlayingVideo: Video? = null

    override suspend fun getVideo(
        id: Int,
        category: Int,
        episodeNumber: Int,
        isComingSoon: Boolean
    ): VideoMedia? {
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
                    score = videoDetails?.score ?: return null,
                    isComingSoon = isComingSoon
                )
            } else {
                Toast.makeText(context, videoMediaResult.message, Toast.LENGTH_SHORT).show()
                null
            }
        } else {
            Toast.makeText(context, videoDetailsResult.message, Toast.LENGTH_SHORT).show()
            null
        }
    }

    override suspend fun getVideoDetails(video: Video): GetVideoDetailsResponse.Data? {
        val result = remoteSource.getVideoDetails(video.contentId, video.category ?: -1)

        return if (result is Result.Success) {
            result.data?.data
        } else {
            Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
            null
        }
    }

    override suspend fun getSeriesEpisodes(video: Video): VideoGroup? {
        val result = remoteSource.getVideoDetails(video.contentId, video.category ?: -1)

        return if (result is Result.Success) {
            val videoDetails = result.data?.data ?: return null
            VideoGroup(
                category = videoDetails.name,
                videoList = videoDetails.episodeVo.map { episodeBean ->
                    Video(video, episodeBean, videoDetails.episodeVo.size, videoDetails.score)
                },
                contentType = VideoGroup.ContentType.VIDEOS
            )
        } else {
            Toast.makeText(context, result.message, Toast.LENGTH_SHORT).show()
            null
        }
    }
}

interface IMediaRepository {
    var currentlyPlayingVideo: Video?

    suspend fun getVideo(
        id: Int,
        category: Int,
        episodeNumber: Int = 0,
        isComingSoon: Boolean = false
    ): VideoMedia?

    suspend fun getVideoDetails(video: Video): GetVideoDetailsResponse.Data?
    suspend fun getSeriesEpisodes(video: Video): VideoGroup?
}