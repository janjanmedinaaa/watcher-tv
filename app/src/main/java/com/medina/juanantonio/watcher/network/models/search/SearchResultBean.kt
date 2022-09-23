package com.medina.juanantonio.watcher.network.models.search

data class SearchResultBean(
    val id: Int,
    val name: String,
    val coverHorizontalUrl: String,
    val coverVerticalUrl: String,

    // It looks like domainType also means "Category ID"
    val domainType: Int
)