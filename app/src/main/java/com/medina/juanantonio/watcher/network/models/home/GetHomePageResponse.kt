package com.medina.juanantonio.watcher.network.models.home

import com.medina.juanantonio.watcher.network.models.ApiResponse

data class GetHomePageResponse(
    private val _code: String,
    private val _data: Data,
    private val _msg: String
) : ApiResponse<GetHomePageResponse.Data>(_code, _data, _msg) {

    inner class Data(
        val page: Int,
        val recommendItems: List<HomePageBean>,
        val searchKeyWord: String
    )
}