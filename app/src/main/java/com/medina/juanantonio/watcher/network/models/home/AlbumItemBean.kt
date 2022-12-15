package com.medina.juanantonio.watcher.network.models.home

data class AlbumItemBean(
    val contentId: String,
    val domainType: Int,
    val dramaType: String,
    val image: String,
    val introduction: String,
    val name: String,
    val releaseTime: String,
    val score: Double
)