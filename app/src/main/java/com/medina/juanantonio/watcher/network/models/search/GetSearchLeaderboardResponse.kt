package com.medina.juanantonio.watcher.network.models.search

data class GetSearchLeaderboardResponse(
    val code: String,
    val data: Data,
    val msg: String
) {

    inner class Data(
        val list: List<LeaderboardBean>
    )
}