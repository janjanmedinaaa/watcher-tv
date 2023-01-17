package com.medina.juanantonio.watcher.network.models.home

import com.medina.juanantonio.watcher.network.models.ApiResponse

data class GetAlbumDetailsResponse(
    private val _code: String,
    private val _data: Data,
    private val _msg: String
) : ApiResponse<GetAlbumDetailsResponse.Data>(_code, _data, _msg) {

    inner class Data(
        val name: String,
        val headImg: String,
        val content: List<AlbumItemBean>
    )
}