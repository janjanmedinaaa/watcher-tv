package com.medina.juanantonio.watcher.network.models.player

import com.google.gson.annotations.JsonAdapter
import com.medina.juanantonio.watcher.network.deserializer.DefinitionCodeDeserializer

data class EpisodeBean(
    val id: Int,
    val definitionList: List<Definition>,
    val seriesNo: Int,
    val subtitlingList: List<Subtitle>
) {

    inner class Definition(
        val code: DefinitionCode,
        val description: String,
        val fullDescription: String
    )

    @JsonAdapter(DefinitionCodeDeserializer::class)
    enum class DefinitionCode {
        GROOT_HD,
        GROOT_SD,
        GROOT_LD,
        GROOT_FD,

        UNKNOWN
    }

    fun getDefinition(): DefinitionCode {
        return definitionList.firstOrNull { definition ->
            definition.code == DefinitionCode.GROOT_HD
        }?.code ?: definitionList.first().code
    }
}