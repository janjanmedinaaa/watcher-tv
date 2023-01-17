package com.medina.juanantonio.watcher.network.models.auth

import com.medina.juanantonio.watcher.network.models.ApiResponse

data class RefreshTokenResponse(
    private val _code: String,
    private val _data: String,
    private val _msg: String
) : ApiResponse<String>(_code, _data, _msg)