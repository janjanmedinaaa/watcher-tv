package com.medina.juanantonio.watcher.github.models

data class GetAccessTokenRequest(
    val repository: String,
    val permissions: Permission
) {

    class Permission(
        val contents: String
    )
}