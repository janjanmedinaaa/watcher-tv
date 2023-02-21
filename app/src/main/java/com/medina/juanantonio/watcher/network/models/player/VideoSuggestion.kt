package com.medina.juanantonio.watcher.network.models.player

data class VideoSuggestion(
    val category: Int,
    val coverHorizontalUrl: String,
    val coverVerticalUrl: String,
    val id: Int,
    val name: String,
    val score: Double,
    val seriesNo: Int
)