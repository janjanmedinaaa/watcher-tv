package com.medina.juanantonio.watcher.network.models.auth

import com.medina.juanantonio.watcher.network.models.ApiResponse

data class LoginResponse(
    private val _code: String,
    private val _data: Data,
    private val _msg: String
) : ApiResponse<LoginResponse.Data>(_code, _data, _msg) {

    inner class Data(
        val token: String
    )
}