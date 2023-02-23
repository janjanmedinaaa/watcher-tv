package com.medina.juanantonio.watcher.network.models.player

import com.google.gson.annotations.JsonAdapter
import com.medina.juanantonio.watcher.network.deserializer.DefinitionCodeDeserializer

data class Definition(
    val code: DefinitionCode,
    val description: String,
    val fullDescription: String
) {

    @JsonAdapter(DefinitionCodeDeserializer::class)
    enum class DefinitionCode {
        GROOT_HD,
        GROOT_SD,
        GROOT_LD,
        GROOT_FD,

        UNKNOWN
    }
}