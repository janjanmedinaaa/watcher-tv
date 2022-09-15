package com.medina.juanantonio.watcher.network.models.search

data class SearchResultBean(
    val id: Int,
    val name: String,
    val coverHorizontalUrl: String,
    val coverVerticalUrl: String,
    val dramaType: DramaType?
) {

    inner class DramaType(
        val code: DramaCode,
        val name: String
    )

    enum class DramaCode {
        MOVIE,
        TV,
        COMIC
    }
}