package com.medina.juanantonio.watcher.network.models.home

import com.medina.juanantonio.watcher.network.models.ApiResponse

data class GetWatchHistoryResponse(
    private val _code: String,
    private val _data: Data,
    private val _msg: String
) : ApiResponse<GetWatchHistoryResponse.Data>(_code, _data, _msg) {

    inner class Data(
        val historyList: List<WatchHistoryBean>?
    )
}