package com.medina.juanantonio.watcher.sources.content

import com.medina.juanantonio.watcher.data.models.video.Video
import com.medina.juanantonio.watcher.data.models.video.VideoMedia
import com.medina.juanantonio.watcher.sources.auth.IAuthRepository
import com.medina.juanantonio.watcher.sources.media.IVideoDatabase
import com.medina.juanantonio.watcher.sources.user.IUserRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchHistoryUseCase @Inject constructor(
    private val authRepository: IAuthRepository,
    private val userRepository: IUserRepository,
    private val database: IVideoDatabase,
    private val tvProviderUseCase: TVProviderUseCase
) {

    companion object {
        private const val VIDEO_ENDED_PERCENTAGE = 95
    }

    suspend fun getLocalOnGoingVideos(): List<Video> {
        return database.getOnGoingVideos()
    }

    suspend fun getOnGoingVideos(): List<Video> {
        if (authRepository.isUserAuthenticated()) {
            val watchHistoryFromAPI = userRepository.getWatchHistory()
            if (watchHistoryFromAPI.isNotEmpty())
                return watchHistoryFromAPI
        }

        return getLocalOnGoingVideos()
    }

    suspend fun addOnGoingVideo(video: Video, videoMedia: VideoMedia) {
        val hasVideoEnded = hasVideoEnd(video, videoMedia)

        if (video.isMovie && hasVideoEnded) {
            tvProviderUseCase.removeVideoFromWatchNextRow(video)
            removeOnGoingVideo(video)
            return
        }

        val durationMillis = videoMedia.totalDuration * 1000
        tvProviderUseCase.addVideoToWatchNextRow(video, durationMillis)
        if (authRepository.isUserAuthenticated()) {
            userRepository.saveWatchHistory(video, videoMedia)
            return
        }

        database.addVideo(video)
    }

    suspend fun getOnGoingVideo(id: Int): Video? {
        if (authRepository.isUserAuthenticated()) {
            val watchHistoryItem = userRepository.watchHistory.firstOrNull {
                it.contentId.toIntOrNull() == id
            }

            if (watchHistoryItem != null)
                return Video(watchHistoryItem)
        }

        return database.getVideo(id)
    }

    suspend fun removeOnGoingVideo(video: Video) {
        tvProviderUseCase.removeVideoFromWatchNextRow(video)
        if (authRepository.isUserAuthenticated()) {
            userRepository.removeWatchHistory(video.contentId, video.category ?: 0)
            return
        }

        database.removeVideo(video.contentId)
    }

    suspend fun clearLocalOnGoingVideos() {
        database.clear()
    }

    private fun hasVideoEnd(video: Video, videoMedia: VideoMedia): Boolean {
        val progress = (video.videoProgress / 1000L).toInt()
        val totalDuration = videoMedia.totalDuration
        val videoWatchedPercentage = ((progress.toFloat() / totalDuration.toFloat()) * 100)

        return videoWatchedPercentage >= VIDEO_ENDED_PERCENTAGE
    }
}