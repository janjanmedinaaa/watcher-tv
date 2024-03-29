package com.medina.juanantonio.watcher.data.models.video

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

/**
 * A group of content that has a String category and a List of Video objects
 */
@Parcelize
class VideoGroup(
    val category: String,
    val videoList: List<Video>,
    val contentType: ContentType
) : Parcelable {

    enum class ContentType {
        VIDEOS,
        ARTISTS,
        TOP_CONTENT,
        COLLECTION,
        MOVIE_LIST,
        COMING_SOON,
        LEADERBOARD
    }
}