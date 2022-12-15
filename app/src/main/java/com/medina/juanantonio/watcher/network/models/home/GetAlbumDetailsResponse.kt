package com.medina.juanantonio.watcher.network.models.home

data class GetAlbumDetailsResponse(
    val code: String,
    val data: Data,
    val msg: String
) {

    inner class Data(
        val name: String,
        val headImg: String,
        val content: List<AlbumItemBean>
    )
}