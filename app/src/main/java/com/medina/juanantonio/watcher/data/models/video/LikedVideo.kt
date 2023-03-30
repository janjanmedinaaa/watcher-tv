package com.medina.juanantonio.watcher.data.models.video

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class LikedVideo(
    val category: Int?,
    @PrimaryKey val contentId: Int,
    val imageUrl: String,
    val title: String
) {

    constructor(video: Video) : this(
        category = video.category,
        contentId = video.contentId,
        imageUrl = video.imageUrl,
        title = video.title
    )
}