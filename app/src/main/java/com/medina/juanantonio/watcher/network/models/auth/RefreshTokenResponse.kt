package com.medina.juanantonio.watcher.network.models.auth

data class RefreshTokenResponse(
    val code: String,
    val data: String,
    val msg: String
)