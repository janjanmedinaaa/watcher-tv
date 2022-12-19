package com.medina.juanantonio.watcher.sources.content

import com.medina.juanantonio.watcher.data.models.Video
import com.medina.juanantonio.watcher.data.models.VideoMedia
import com.medina.juanantonio.watcher.sources.auth.IAuthRepository
import com.medina.juanantonio.watcher.sources.media.IVideoDatabase
import com.medina.juanantonio.watcher.sources.user.IUserRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WatchHistoryUseCase @Inject constructor(
    private val authRepository: IAuthRepository,
    private val userRepository: IUserRepository,
    private val database: IVideoDatabase
) {

    suspend fun getOnGoingVideos(): List<Video> {
        if (authRepository.isUserAuthenticated()) {
            val watchHistoryFromAPI = userRepository.getWatchHistory()
            if (watchHistoryFromAPI.isNotEmpty())
                return watchHistoryFromAPI
        }

        return database.getOnGoingVideos()
    }

    suspend fun addOnGoingVideo(video: Video, videoMedia: VideoMedia) {
        if (authRepository.isUserAuthenticated()) {
            userRepository.saveWatchHistory(video, videoMedia)
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

    suspend fun removeOnGoingVideo(id: Int) {
        if (authRepository.isUserAuthenticated()) {
            userRepository.removeWatchHistory(id)
        }

        database.removeVideo(id)
    }

    suspend fun clearLocalCacheVideos() {
        database.clear()
    }
}