package com.medina.juanantonio.watcher.network.models.player

import com.medina.juanantonio.watcher.network.models.ApiResponse

data class GetVideoDetailsResponse(
    private val _code: String,
    private val _data: Data,
    private val _msg: String
) : ApiResponse<GetVideoDetailsResponse.Data>(_code, _data, _msg) {

    inner class Data(
        val coverHorizontalUrl: String,
        val coverVerticalUrl: String,
        val name: String,
        val introduction: String,
        val episodeCount: Int?,
        val episodeVo: List<EpisodeBean>,
        val score: Double,
        val seriesNo: Int?,
        val year: Int,
        val tagNameList: List<String>,

        // Movie/Series Suggestions
        val likeList: List<VideoSuggestion>,

        // Movies/Series seasons directly related to Movie list (e.g Trilogies, Next Seasons)
        val refList: List<VideoSuggestion>
    )
}