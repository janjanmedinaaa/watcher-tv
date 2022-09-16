package com.medina.juanantonio.watcher.sources.home

import com.medina.juanantonio.watcher.data.models.Video
import com.medina.juanantonio.watcher.database.WatcherDb

class HomePageDatabase(watcherDb: WatcherDb) : IHomePageDatabase {
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
}

interface IHomePageDatabase {
    suspend fun addVideo(video: Video)
    suspend fun getOnGoingVideos(): List<Video>
    suspend fun getVideo(id: Int): Video?
    suspend fun removeVideo(id: Int)
}