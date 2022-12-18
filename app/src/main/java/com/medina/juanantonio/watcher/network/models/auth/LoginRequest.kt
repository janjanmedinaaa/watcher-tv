package com.medina.juanantonio.watcher.network.models.auth

data class LoginRequest(
    val mobile: String,
    val countryCode: String,
    val captcha: String,
    val phoneModel: String,
    val phoneSystem: String,
    val adjustId: String
)