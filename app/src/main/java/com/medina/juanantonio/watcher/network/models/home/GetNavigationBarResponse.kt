package com.medina.juanantonio.watcher.network.models.home

import com.medina.juanantonio.watcher.network.models.ApiResponse

data class GetNavigationBarResponse(
    private val _code: String,
    private val _data: Data,
    private val _msg: String
) : ApiResponse<GetNavigationBarResponse.Data>(_code, _data, _msg) {

    inner class Data(
        val navigationBarItemList: List<NavigationItemBean>,
    )
}