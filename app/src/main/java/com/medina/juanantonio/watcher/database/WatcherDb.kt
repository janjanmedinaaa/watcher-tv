package com.medina.juanantonio.watcher.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.medina.juanantonio.watcher.BuildConfig
import com.medina.juanantonio.watcher.data.models.Video
import com.medina.juanantonio.watcher.database.dao.VideoDao

@Database(
    entities = [
        Video::class
    ],
    version = BuildConfig.VERSION_CODE,
    exportSchema = true
)
abstract class WatcherDb : RoomDatabase() {
    abstract fun videoDao(): VideoDao
}