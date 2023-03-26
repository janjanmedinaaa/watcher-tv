package com.medina.juanantonio.watcher.data.models.video

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class LikedVideo(
    @PrimaryKey val contentId: Int
)