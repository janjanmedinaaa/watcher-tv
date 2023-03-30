package com.medina.juanantonio.watcher.sources.media

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.medina.juanantonio.watcher.data.models.video.Video
import com.medina.juanantonio.watcher.data.models.video.VideoGroup
import com.medina.juanantonio.watcher.data.models.video.VideoMedia
import com.medina.juanantonio.watcher.network.Result
import com.medina.juanantonio.watcher.network.models.player.GetVideoDetailsResponse
import com.medina.juanantonio.watcher.network.models.player.GetVideoResourceResponse
import com.medina.juanantonio.watcher.shared.extensions.toastIfNotBlank
import com.medina.juanantonio.watcher.shared.utils.Event
import com.medina.juanantonio.watcher.sources.content.LikedVideoUseCase

class MediaRepository(
    private val context: Context,
    private val remoteSource: IMediaRemoteSource,
    private val likedVideoUseCase: LikedVideoUseCase
) : IMediaRepository {

    private var _currentlyPlayingVideo: Video? = null
    override val currentlyPlayingVideo: Video?
        get() = _currentlyPlayingVideo

    override val currentlyPlayingVideoMedia: VideoMedia?
        get() = videoMediaLiveData.value?.peek()

    override val videoMediaLiveData = MutableLiveData<Event<VideoMedia>>()

    override fun setCurrentlyPlaying(video: Video, videoMedia: VideoMedia) {
        _currentlyPlayingVideo = video
        videoMediaLiveData.value = Event(videoMedia)
    }

    override suspend fun getVideoResource(
        category: Int,
        contentId: Int,
        episodeId: Int,
        definition: String
    ): GetVideoResourceResponse.Data? {
        return remoteSource.getVideoResource(
            category = category,
            contentId = contentId,
            episodeId = episodeId,
            definition = definition
        ).data?.data
    }

    override suspend fun getVideo(
        id: Int,
        category: Int,
        definition: String?,
        episodeNumber: Int,
        isComingSoon: Boolean
    ): VideoMedia? {
        val isLikedVideo = likedVideoUseCase.checkLikedVideo(id)
        val videoDetailsResult = remoteSource.getVideoDetails(id, category)

        return if (videoDetailsResult is Result.Success) {
            val videoDetails = videoDetailsResult.data?.data
            val episode = if (episodeNumber == 0) {
                videoDetails?.episodeVo?.firstOrNull()
            } else {
                videoDetails?.episodeVo?.firstOrNull { it.seriesNo == episodeNumber }
            }
            val chosenDefinition = definition ?: episode?.getDefaultDefinition()?.code?.name

            val videoMediaResult = remoteSource.getVideoResource(
                category = category,
                contentId = id,
                episodeId = episode?.id ?: -1,
                definition = chosenDefinition ?: ""
            )

            if (videoMediaResult is Result.Success) {
                VideoMedia(
                    contentId = id,
                    categoryId = category,
                    episodeBean = episode ?: return null,
                    detailsResponse = videoDetailsResult.data?.data ?: return null,
                    mediaResponse = videoMediaResult.data?.data ?: return null,
                    score = videoDetails?.score ?: return null,
                    isComingSoon = isComingSoon,
                    isLikedVideo = isLikedVideo
                )
            } else {
                videoMediaResult.message.toastIfNotBlank(context)
                null
            }
        } else {
            videoDetailsResult.message.toastIfNotBlank(context)
            null
        }
    }

    override suspend fun getVideoDetails(video: Video): GetVideoDetailsResponse.Data? {
        val result = remoteSource.getVideoDetails(video.contentId, video.category ?: -1)

        return if (result is Result.Success) {
            result.data?.data
        } else {
            result.message.toastIfNotBlank(context)
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
            result.message.toastIfNotBlank(context)
            null
        }
    }
}

interface IMediaRepository {
    val currentlyPlayingVideo: Video?
    val currentlyPlayingVideoMedia: VideoMedia?
    val videoMediaLiveData: LiveData<Event<VideoMedia>>

    fun setCurrentlyPlaying(video: Video, videoMedia: VideoMedia)
    suspend fun getVideoResource(
        category: Int,
        contentId: Int,
        episodeId: Int,
        definition: String
    ): GetVideoResourceResponse.Data?

    suspend fun getVideo(
        id: Int,
        category: Int,
        definition: String? = null,
        episodeNumber: Int = 0,
        isComingSoon: Boolean = false
    ): VideoMedia?

    suspend fun getVideoDetails(video: Video): GetVideoDetailsResponse.Data?
    suspend fun getSeriesEpisodes(video: Video): VideoGroup?
}