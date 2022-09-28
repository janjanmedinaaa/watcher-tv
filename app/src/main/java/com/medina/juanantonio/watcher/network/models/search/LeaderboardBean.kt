package com.medina.juanantonio.watcher.network.models.search

data class LeaderboardBean(
    val id: Int,
    val title: String,
    val cover: String,

    // It looks like domainType also means "Category ID"
    val domainType: Int
)