package com.medina.juanantonio.watcher.network.models.search

data class SearchByKeywordResponse(
    val code: String,
    val data: Data,
    val msg: String
) {

    inner class Data(
        val searchResults: List<SearchResultBean>,
        val searchType: String
    )
}