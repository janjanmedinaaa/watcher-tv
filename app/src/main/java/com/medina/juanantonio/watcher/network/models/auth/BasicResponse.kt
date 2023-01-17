package com.medina.juanantonio.watcher.network.models.auth

import com.medina.juanantonio.watcher.network.models.ApiResponse

data class BasicResponse(
    private val _code: String,
    private val _msg: String
) : ApiResponse<Nothing?>(_code, null, _msg)