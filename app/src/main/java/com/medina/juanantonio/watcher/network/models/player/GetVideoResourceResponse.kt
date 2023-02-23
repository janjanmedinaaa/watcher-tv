package com.medina.juanantonio.watcher.network.models.player

import com.medina.juanantonio.watcher.network.models.ApiResponse

data class GetVideoResourceResponse(
    private val _code: String,
    private val _data: Data,
    private val _msg: String
) : ApiResponse<GetVideoResourceResponse.Data>(_code, _data, _msg) {

    inner class Data(
        val businessType: Int,
        val currentDefinition: Definition.DefinitionCode,
        val episodeId: String,
        val mediaUrl: String,
        val totalDuration: Int
    )
}