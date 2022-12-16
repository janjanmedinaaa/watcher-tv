package com.medina.juanantonio.watcher.network.models.home

data class GetNavigationBarResponse(
    val code: String,
    val data: Data,
    val msg: String
) {

    inner class Data(
        val navigationBarItemList: List<NavigationItemBean>,
    )
}