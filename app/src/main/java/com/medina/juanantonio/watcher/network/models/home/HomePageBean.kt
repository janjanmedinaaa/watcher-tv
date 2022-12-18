package com.medina.juanantonio.watcher.network.models.home

import android.net.Uri
import com.google.gson.annotations.SerializedName

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
        private val jumpAddress: String,
        val needLogin: Boolean,
        val score: Double,
        val title: String,
        val resourceNum: Int?,
        val resourceStatus: ResourceStatus?
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

    enum class ContentType(val category: Int?) {
        MOVIE(0),
        DRAMA(1),
        ALBUM(null),
        APP_URL(null)
    }

    enum class ResourceStatus {
        @SerializedName("1")
        UPDATED,

        @SerializedName("2")
        TOTAL
    }
}