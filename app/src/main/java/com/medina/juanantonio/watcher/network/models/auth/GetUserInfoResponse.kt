package com.medina.juanantonio.watcher.network.models.auth

import com.medina.juanantonio.watcher.network.models.ApiResponse

data class GetUserInfoResponse(
    private val _code: String,
    private val _data: Data,
    private val _msg: String
) : ApiResponse<GetUserInfoResponse.Data?>(_code, _data, _msg) {

    inner class Data(
        val userId: String,
        val headImgUrl: String,
        val nickName: String,
        val countryCode: String
    )
}