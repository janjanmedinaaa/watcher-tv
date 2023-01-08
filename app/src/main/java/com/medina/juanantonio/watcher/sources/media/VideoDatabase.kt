package com.medina.juanantonio.watcher.sources.media

import com.medina.juanantonio.watcher.data.models.video.Video
import com.medina.juanantonio.watcher.database.WatcherDb

class VideoDatabase(watcherDb: WatcherDb) : IVideoDatabase {
    private val videoDao = watcherDb.videoDao()

    override suspend fun addVideo(video: Video) {
        videoDao.insert(video)
    }

    override suspend fun getOnGoingVideos(): List<Video> {
        return videoDao.getAll()
    }

    override suspend fun getVideo(id: Int): Video? {
        return videoDao.getVideo(id)
    }

    override suspend fun removeVideo(id: Int) {
        videoDao.delete(id)
    }

    override suspend fun clear() {
        videoDao.clear()
    }
}

interface IVideoDatabase {
    suspend fun addVideo(video: Video)
    suspend fun getOnGoingVideos(): List<Video>
    suspend fun getVideo(id: Int): Video?
    suspend fun removeVideo(id: Int)
    suspend fun clear()
}