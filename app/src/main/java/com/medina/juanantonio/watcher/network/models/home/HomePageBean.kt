package com.medina.juanantonio.watcher.network.models.home

import android.net.Uri
import com.google.gson.annotations.JsonAdapter
import com.medina.juanantonio.watcher.network.deserializer.ContentTypeDeserializer
import com.medina.juanantonio.watcher.network.deserializer.ResourceStatusDeserializer
import com.medina.juanantonio.watcher.network.deserializer.SectionTypeDeserializer

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
        val resourceStatus: ResourceStatus?,
        val onlineTime: String?
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

    @JsonAdapter(SectionTypeDeserializer::class)
    enum class SectionType {
        BANNER,
        SINGLE_ALBUM,
        BLOCK_GROUP,
        MOVIE_RESERVE,

        UNKNOWN
    }

    @JsonAdapter(ContentTypeDeserializer::class)
    enum class ContentType(val category: Int?) {
        MOVIE(0),
        DRAMA(1),
        ALBUM(null),
        APP_URL(null),

        UNKNOWN(null)
    }

    @JsonAdapter(ResourceStatusDeserializer::class)
    enum class ResourceStatus {
        UPDATED,
        TOTAL,
        UNKNOWN
    }
}