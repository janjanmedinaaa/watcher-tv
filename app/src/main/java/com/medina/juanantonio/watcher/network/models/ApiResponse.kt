package com.medina.juanantonio.watcher.network.models

import com.google.gson.annotations.SerializedName

open class ApiResponse<T>(
    @SerializedName("code")
    open val code: String,

    @SerializedName("data")
    open val data: T,

    @SerializedName("msg")
    open val msg: String
)