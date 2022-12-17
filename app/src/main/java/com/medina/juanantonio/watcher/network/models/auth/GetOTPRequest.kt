package com.medina.juanantonio.watcher.network.models.auth

data class GetOTPRequest(
    val mobile: String,
    val countryCode: String,
    val sendType: String
)