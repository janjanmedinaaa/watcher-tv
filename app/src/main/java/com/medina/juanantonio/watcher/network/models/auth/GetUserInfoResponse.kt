package com.medina.juanantonio.watcher.network.models.auth

data class GetUserInfoResponse(
    val code: String,
    val data: Data?,
    val msg: String
) {

    inner class Data(
        val userId: String,
        val headImgUrl: String,
        val nickName: String
    )
}