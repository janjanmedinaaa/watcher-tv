package com.medina.juanantonio.watcher.sources.media

import com.medina.juanantonio.watcher.data.models.video.LikedVideo
import com.medina.juanantonio.watcher.database.WatcherDb
import kotlinx.coroutines.flow.Flow

class LikedVideoDatabase(watcherDb: WatcherDb) : ILikedVideoDatabase {
    private val likedVideoDao = watcherDb.likedVideoDao()

    override suspend fun addLikedVideo(likedVideo: LikedVideo) {
        likedVideoDao.insert(likedVideo)
    }

    override suspend fun getLikedVideos(): List<LikedVideo> {
        return likedVideoDao.getAll()
    }

    override fun getLikedVideo(id: Int): Flow<LikedVideo?> {
        return likedVideoDao.getLikedVideoUntilChanged(id)
    }

    override suspend fun removeLikedVideo(id: Int) {
        likedVideoDao.delete(id)
    }

    override suspend fun clear() {
        likedVideoDao.clear()
    }
}

interface ILikedVideoDatabase {
    suspend fun addLikedVideo(likedVideo: LikedVideo)
    suspend fun getLikedVideos(): List<LikedVideo>
    fun getLikedVideo(id: Int): Flow<LikedVideo?>
    suspend fun removeLikedVideo(id: Int)
    suspend fun clear()
}