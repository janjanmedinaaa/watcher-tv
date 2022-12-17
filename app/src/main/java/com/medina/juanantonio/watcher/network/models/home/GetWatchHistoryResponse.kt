package com.medina.juanantonio.watcher.network.models.home

data class GetWatchHistoryResponse(
    val code: String,
    val data: Data,
    val msg: String
) {

    inner class Data(
        val historyList: List<WatchHistoryBean>?
    )
}