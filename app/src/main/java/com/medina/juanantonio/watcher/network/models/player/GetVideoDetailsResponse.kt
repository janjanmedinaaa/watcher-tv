package com.medina.juanantonio.watcher.network.models.player

data class GetVideoDetailsResponse(
    val code: String,
    val data: Data,
    val msg: String
) {

    inner class Data(
        val coverHorizontalUrl: String,
        val coverVerticalUrl: String,
        val name: String,
        val introduction: String,
        val episodeCount: Int?,
        val episodeVo: List<EpisodeBean>
    )
}