package com.medina.juanantonio.watcher.data.models

import android.os.Parcelable
import com.medina.juanantonio.watcher.network.models.home.HomePageBean
import com.medina.juanantonio.watcher.network.models.player.EpisodeBean
import com.medina.juanantonio.watcher.network.models.search.SearchResultBean
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Video(
    val category: Int?,
    val contentType: HomePageBean.ContentType,
    val contentId: Int,
    val imageUrl: String,
    val title: String,
    val episodeId: Int
) : Parcelable {

    var isSearchResult = false

    constructor(bean: HomePageBean.Content) : this(
        category = bean.category,
        contentType = bean.contentType,
        contentId = bean.id,
        imageUrl = bean.imageUrl,
        title = bean.title,
        episodeId = 0
    )

    constructor(video: Video, bean: EpisodeBean) : this(
        category = video.category,
        contentType = video.contentType,
        contentId = video.contentId,
        imageUrl = video.imageUrl,
        title = "Episode ${bean.seriesNo}",
        episodeId = bean.seriesNo
    )

    constructor(bean: SearchResultBean) : this(
        category = when (bean.dramaType?.code) {
            SearchResultBean.DramaCode.MOVIE -> 0
            else -> 1
        },
        contentType = when (bean.dramaType?.code) {
            SearchResultBean.DramaCode.MOVIE -> HomePageBean.ContentType.MOVIE
            else -> HomePageBean.ContentType.DRAMA
        },
        contentId = bean.id,
        imageUrl = bean.coverVerticalUrl,
        title = bean.name,
        episodeId = 0
    ) {
        isSearchResult = true
    }
}