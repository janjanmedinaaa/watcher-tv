package com.medina.juanantonio.watcher.sources.user

import com.medina.juanantonio.watcher.data.models.Video
import com.medina.juanantonio.watcher.data.models.VideoMedia
import com.medina.juanantonio.watcher.network.Result
import com.medina.juanantonio.watcher.network.models.auth.GetUserInfoResponse
import com.medina.juanantonio.watcher.network.models.home.WatchHistoryBean

class UserRepository(
    private val remoteSource: IUserRemoteSource
) : IUserRepository {

    override val watchHistory: ArrayList<WatchHistoryBean> = arrayListOf()

    override suspend fun getUserInfo(): GetUserInfoResponse.Data? {
        val result = remoteSource.getUserInfo()

        return if (result is Result.Success) {
            result.data?.data
        } else null
    }

    override suspend fun getWatchHistory(): List<Video> {
        val result = remoteSource.getWatchHistory()

        return if (result is Result.Success) {
            val resultData = result.data?.data

            resultData?.historyList?.run {
                watchHistory.clear()
                watchHistory.addAll(this)

                map { Video(it) }
            } ?: emptyList()
        } else emptyList()
    }

    override suspend fun saveWatchHistory(video: Video, videoMedia: VideoMedia) {
        remoteSource.saveWatchHistory(
            category = videoMedia.categoryId,
            contentId = videoMedia.contentId,
            contentEpisodeId = videoMedia.id,
            progress = (video.videoProgress / 1000L).toInt(),
            totalDuration = videoMedia.totalDuration,
            timestamp = video.lastWatchTime,
            seriesNo = videoMedia.seriesNo,
            episodeNo = video.episodeNumber
        )
    }

    override suspend fun removeWatchHistory(id: Int, category: Int) {
        remoteSource.deleteWatchHistory(
            contentId = id,
            category = category,
        )
    }
}

interface IUserRepository {
    val watchHistory: List<WatchHistoryBean>

    suspend fun getUserInfo(): GetUserInfoResponse.Data?
    suspend fun getWatchHistory(): List<Video>
    suspend fun saveWatchHistory(
        video: Video,
        videoMedia: VideoMedia
    )

    suspend fun removeWatchHistory(id: Int, category: Int)
}