package com.medina.juanantonio.watcher.network.models.home

data class GetHomePageResponse(
    val code: String,
    val data: Data,
    val msg: String
) {

    inner class Data(
        val page: Int,
        val recommendItems: List<HomePageBean>,
        val searchKeyWord: String
    )
}