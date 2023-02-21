package com.medina.juanantonio.watcher.network.models.player

data class EpisodeBean(
    val id: Int,
    val definitionList: List<Definition>,
    val seriesNo: Int,
    val subtitlingList: List<Subtitle>
) {

    fun getDefaultDefinition(): Definition {
        return definitionList.firstOrNull { definition ->
            definition.code == Definition.DefinitionCode.GROOT_HD
        } ?: definitionList.first()
    }
}