package com.medina.juanantonio.watcher.network.models.home

data class WatchHistoryBean(
    val contentId: String,
    val category: Int,
    val seriesNo: String?,
    val contentEpisodeId: String,
    val episodeNo: Int,
    val contentTitle: String,
    val progress: Int,
    val totalDuration: Int,
    val timeStamp: Long,
    val verticalUrl: String
)