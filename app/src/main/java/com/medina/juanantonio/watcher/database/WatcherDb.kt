package com.medina.juanantonio.watcher.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import com.medina.juanantonio.watcher.data.models.user.User
import com.medina.juanantonio.watcher.data.models.video.LikedVideo
import com.medina.juanantonio.watcher.data.models.video.Video
import com.medina.juanantonio.watcher.database.dao.LikedVideoDao
import com.medina.juanantonio.watcher.database.dao.UserDao
import com.medina.juanantonio.watcher.database.dao.VideoDao

@Database(
    entities = [
        Video::class,
        User::class,
        LikedVideo::class
    ],
    version = WatcherDb.VERSION_CODE,
    autoMigrations = [
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5)
    ],
    exportSchema = true
)
abstract class WatcherDb : RoomDatabase() {
    abstract fun videoDao(): VideoDao
    abstract fun userDao(): UserDao
    abstract fun likedVideoDao(): LikedVideoDao

    companion object {
        const val VERSION_CODE = 5

        val MIGRATION_2_3 = Migration(2, 3) {}
    }
}