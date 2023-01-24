package com.medina.juanantonio.watcher.network.models.search

data class AlbumResultBean(
    val contents: List<SearchResultBean>,
    val id: Int,
    val name: String,
    val sort: String,
    val style: Int
)