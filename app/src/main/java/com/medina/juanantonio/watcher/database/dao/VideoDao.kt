package com.medina.juanantonio.watcher.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.medina.juanantonio.watcher.data.models.Video

@Dao
interface VideoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(video: Video)

    @Query("SELECT * FROM video")
    suspend fun getAll(): List<Video>

    @Query("SELECT * FROM video WHERE contentId = :id")
    suspend fun getVideo(id: Int): Video?

    @Query("DELETE FROM video WHERE contentId = :id")
    suspend fun delete(id: Int)

    @Query("DELETE FROM video")
    suspend fun clear()
}