package com.medina.juanantonio.watcher.network.models.search

data class SearchByKeywordRequest(
    val searchKeyword: String,
    val size: Int = 50,
    val sort: String = "",
    val searchType: String = ""
)