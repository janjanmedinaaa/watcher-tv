package com.medina.juanantonio.watcher.network.models.search

import com.medina.juanantonio.watcher.network.models.ApiResponse

data class SearchByKeywordResponse(
    private val _code: String,
    private val _data: Data,
    private val _msg: String
) : ApiResponse<SearchByKeywordResponse.Data>(_code, _data, _msg) {

    inner class Data(
        val searchResults: List<SearchResultBean>,
        val searchType: String
    )
}