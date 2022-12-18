package com.medina.juanantonio.watcher.network.models.auth

data class LoginResponse(
    val code: String,
    val data: Data,
    val msg: String
) {

    inner class Data(
        val token: String
    )
}