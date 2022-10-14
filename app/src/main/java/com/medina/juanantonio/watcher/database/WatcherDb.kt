package com.medina.juanantonio.watcher.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import com.medina.juanantonio.watcher.data.models.Video
import com.medina.juanantonio.watcher.database.dao.VideoDao

@Database(
    entities = [
        Video::class
    ],
    version = WatcherDb.VERSION_CODE,
    exportSchema = true
)
abstract class WatcherDb : RoomDatabase() {
    abstract fun videoDao(): VideoDao

    companion object {
        const val VERSION_CODE = 3

        val MIGRATION_2_3 = Migration(2, 3) {}
    }
}