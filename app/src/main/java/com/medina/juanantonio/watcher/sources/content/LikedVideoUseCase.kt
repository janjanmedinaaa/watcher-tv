package com.medina.juanantonio.watcher.sources.content

import com.medina.juanantonio.watcher.data.models.video.LikedVideo
import com.medina.juanantonio.watcher.sources.media.ILikedVideoDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LikedVideoUseCase @Inject constructor(
    private val likedVideoDatabase: ILikedVideoDatabase
) {

    suspend fun addLikedVideo(contentId: Int) {
        likedVideoDatabase.addLikedVideo(LikedVideo(contentId))
    }

    fun getLikedVideo(contentId: Int): Flow<LikedVideo?> {
        return likedVideoDatabase.getLikedVideo(contentId)
    }

    suspend fun checkLikedVideo(contentId: Int): Boolean {
        return likedVideoDatabase.getLikedVideo(contentId).first() != null
    }

    suspend fun removeLikedVideo(contentId: Int) {
        likedVideoDatabase.removeLikedVideo(contentId)
    }

    suspend fun clearLocalLikedVideos() {
        likedVideoDatabase.clear()
    }
}