package com.medina.juanantonio.watcher.data.models

import android.os.Parcelable
import com.medina.juanantonio.watcher.network.models.player.EpisodeBean
import com.medina.juanantonio.watcher.network.models.player.GetVideoDetailsResponse
import com.medina.juanantonio.watcher.network.models.player.GetVideoResourceResponse
import com.medina.juanantonio.watcher.network.models.player.Subtitle
import kotlinx.android.parcel.Parcelize

@Parcelize
data class VideoMedia(
    val id: Int,
    val contentId: Int,
    val categoryId: Int,
    val title: String,
    val introduction: String,
    val mediaUrl: String,
    val subtitles: List<Subtitle>?
) : Parcelable {

    constructor(
        contentId: Int,
        categoryId: Int,
        episodeBean: EpisodeBean,
        detailsResponse: GetVideoDetailsResponse.Data,
        mediaResponse: GetVideoResourceResponse.Data
    ) : this(
        id = episodeBean.id,
        contentId = contentId,
        categoryId = categoryId,
        title = if (categoryId == 0) detailsResponse.name
        else "${detailsResponse.name} - Episode ${episodeBean.seriesNo}",
        introduction = detailsResponse.introduction,
        mediaUrl = mediaResponse.mediaUrl,
        subtitles = detailsResponse.episodeVo.firstOrNull {
            it.id == episodeBean.id
        }?.subtitlingList
    )

    fun getPreferredSubtitle(): Subtitle? {
        return subtitles?.firstOrNull { it.languageAbbr == "en" } ?: subtitles?.firstOrNull()
    }
}