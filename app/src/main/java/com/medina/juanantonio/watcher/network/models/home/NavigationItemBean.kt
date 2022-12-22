package com.medina.juanantonio.watcher.network.models.home

import com.google.gson.annotations.JsonAdapter
import com.medina.juanantonio.watcher.network.deserializer.RedirectContentTypeDeserializer

data class NavigationItemBean(
    val id: Int,
    val name: String,
    val redirectContentType: RedirectContentType,
    val sequence: Int
) {

    @JsonAdapter(RedirectContentTypeDeserializer::class)
    enum class RedirectContentType {
        HOME,
        APP_URL,

        UNKNOWN
    }
}