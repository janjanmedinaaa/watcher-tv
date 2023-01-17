package com.medina.juanantonio.watcher.network.models.search

import com.medina.juanantonio.watcher.network.models.ApiResponse

data class GetSearchLeaderboardResponse(
    private val _code: String,
    private val _data: Data,
    private val _msg: String
) : ApiResponse<GetSearchLeaderboardResponse.Data>(_code, _data, _msg) {

    inner class Data(
        val list: List<LeaderboardBean>
    )
}