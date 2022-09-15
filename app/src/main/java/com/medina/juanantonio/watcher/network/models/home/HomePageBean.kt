package com.medina.juanantonio.watcher.network.models.home

data class HomePageBean(
    val bannerProportion: Double,
    val homeSectionName: String,
    val homeSectionType: SectionType,
    val recommendContentVOList: List<Content>
) {

    inner class Content(
        val category: Int?,
        val contentType: ContentType,
        val id: Int,
        val imageUrl: String,
        val jumpAddress: String,
        val needLogin: Boolean,
        val score: Double,
        val title: String
    )

    enum class SectionType {
        BANNER,
        SINGLE_ALBUM,
        BLOCK_GROUP,
        MOVIE_RESERVE
    }

    enum class ContentType {
        APP_URL,
        MOVIE,
        DRAMA
    }
}