package com.medina.juanantonio.watcher.network.models.player

data class GetVideoResourceResponse(
    val code: String,
    val data: Data,
    val msg: String
) {

    inner class Data(
        val businessType: Int,
        val currentDefinition: EpisodeBean.DefinitionCode,
        val episodeId: String,
        val mediaUrl: String,
        val totalDuration: Int
    )
}