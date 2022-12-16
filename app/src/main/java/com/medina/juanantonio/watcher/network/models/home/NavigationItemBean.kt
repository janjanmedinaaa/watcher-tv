package com.medina.juanantonio.watcher.network.models.home

data class NavigationItemBean(
    val id: Int,
    val name: String,
    val redirectContentType: RedirectContentType,
    val sequence: Int
) {

    enum class RedirectContentType {
        HOME,
        APP_URL
    }
}