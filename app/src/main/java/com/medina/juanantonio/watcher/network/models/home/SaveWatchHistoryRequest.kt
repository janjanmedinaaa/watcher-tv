package com.medina.juanantonio.watcher.network.models.home

data class SaveWatchHistoryRequest(
    val category: Int,
    val contentId: Int,
    val contentEpisodeId: Int,
    val progress: Int,
    val totalDuration: Int,
    val timestamp: Long,
    val playTime: Int,
    val seriesNo: Int?,
    val episodeNo: Int
)