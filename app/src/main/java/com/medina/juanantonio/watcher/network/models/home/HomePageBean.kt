package com.medina.juanantonio.watcher.network.models.home

import android.net.Uri

data class HomePageBean(
    val bannerProportion: Double,
    val homeSectionName: String,
    val homeSectionType: SectionType,
    val recommendContentVOList: List<Content>
) {

    inner class Content(
        val category: Int?,
        val id: Int,
        val imageUrl: String,
        private val jumpAddress: String,
        val needLogin: Boolean,
        val score: Double,
        val title: String,
        val resourceNum: Int?
    ) {

        fun getIdFromJumpAddress(): Int {
            return try {
                val uri = Uri.parse(jumpAddress)
                val idParam = uri.getQueryParameter("id")
                idParam?.toInt() ?: -1
            } catch (e: Exception) {
                -1
            }
        }
    }

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