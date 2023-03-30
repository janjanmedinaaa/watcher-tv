package com.medina.juanantonio.watcher.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.medina.juanantonio.watcher.data.models.video.LikedVideo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged

@Dao
interface LikedVideoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(likedVideo: LikedVideo)

    @Query("SELECT * FROM likedVideo")
    suspend fun getAll(): List<LikedVideo>

    @Query("SELECT * FROM likedVideo WHERE contentId = :id")
    fun getLikedVideo(id: Int): Flow<LikedVideo?>

    fun getLikedVideoUntilChanged(id: Int) =
        getLikedVideo(id).distinctUntilChanged()

    @Query("DELETE FROM likedVideo WHERE contentId = :id")
    suspend fun delete(id: Int)

    @Query("DELETE FROM likedVideo")
    suspend fun clear()
}