package com.medina.juanantonio.watcher.network.models.player

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

    enum class DefinitionCode {
        GROOT_HD,
        GROOT_SD,
        GROOT_LD,
        GROOT_FD
    }
}